package com.back.motionit.domain.challenge.video.external.youtube

import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata
import com.back.motionit.global.error.code.ChallengeVideoErrorCode
import com.back.motionit.global.error.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

@Component
@Profile("!perf & !test")
open class YoutubeMetadataClient(
    @Value("\${youtube.api.key}")
    private val apiKey: String
) {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(YOUTUBE_API_URL)
        .build()

    open fun fetchMetadata(youtubeUrl: String): YoutubeVideoMetadata {
        val videoId = extractVideoId(youtubeUrl)

        val response: Map<*, *> = restClient.get()
            .uri { builder ->
                builder
                    .queryParam("id", videoId)
                    .queryParam("part", "snippet,contentDetails")
                    .queryParam("key", apiKey)
                    .build()
            }
            .retrieve()
            .body(Map::class.java)
            ?: throw BusinessException(ChallengeVideoErrorCode.INVALID_VIDEO_FORMAT)

        val items = response["items"] as? List<Map<*, *>>
            ?: throw BusinessException(ChallengeVideoErrorCode.INVALID_VIDEO_FORMAT)

        if (items.isEmpty()) {
            throw BusinessException(ChallengeVideoErrorCode.INVALID_VIDEO_FORMAT)
        }

        val item = items.first()
        val snippet = item["snippet"] as Map<*, *>
        val thumbnails = snippet["thumbnails"] as Map<*, *>
        val highThumb = thumbnails["high"] as Map<*, *>
        val contentDetails = item["contentDetails"] as Map<*, *>

        return YoutubeVideoMetadata(
            videoId = videoId,
            title = snippet["title"] as String,
            thumbnailUrl = highThumb["url"] as String,
            durationSeconds = parseDuration(contentDetails["duration"] as String)
        )
    }

    fun extractVideoId(youtubeUrl: String): String {
        if ("v=" in youtubeUrl) {
            return youtubeUrl.substringAfter("v=").substringBefore("&")
        }
        throw BusinessException(ChallengeVideoErrorCode.INVALID_VIDEO_FORMAT)
    }

    private fun parseDuration(isoDuration: String): Int =
        Duration.parse(isoDuration).seconds.toInt()

    companion object {
        private const val YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/videos"
    }
}
