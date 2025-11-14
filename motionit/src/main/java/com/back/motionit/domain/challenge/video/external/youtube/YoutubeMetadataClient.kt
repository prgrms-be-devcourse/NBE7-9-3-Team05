package com.back.motionit.domain.challenge.video.external.youtube

import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata
import com.back.motionit.global.error.code.ChallengeVideoErrorCode
import com.back.motionit.global.error.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Component
@Profile("!perf & !test")
open class YoutubeMetadataClient(
    @Value("\${youtube.api.key}")
    private val apiKey: String
) {

    private val restTemplate = RestTemplate()

    // videoId를 추출하고 메타데이터를 가져오는 메서드
    open fun fetchMetadata(youtubeUrl: String): YoutubeVideoMetadata {
        // video ID 추출
        val videoId = extractVideoId(youtubeUrl)

        // YouTube Data API 호출 URL 구성
        val url: String = "$YOUTUBE_API_URL?id=$videoId&part=snippet,contentDetails&key=$apiKey"

        val response: Map<*, *> =
            restTemplate.getForObject(url, Map::class.java)
                ?: throw BusinessException(ChallengeVideoErrorCode.INVALID_VIDEO_FORMAT)

        // 응답에서 필요한 데이터 추출
        val items = response["items"] as? List<Map<*, *>>
            ?: throw BusinessException(ChallengeVideoErrorCode.INVALID_VIDEO_FORMAT)

        if (items.isEmpty()) {
            throw BusinessException(ChallengeVideoErrorCode.INVALID_VIDEO_FORMAT)
        }

        val item = items.first()

        // 데이터 추출 및 YoutubeVideoMetadata dto에 매핑
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

    // 유튜브 URL에서 비디오 ID를 추출하는 헬퍼 메서드, watch?v=VIDEO_ID 형식 가정
    fun extractVideoId(youtubeUrl: String): String {
        if ("v=" in youtubeUrl) {
            return youtubeUrl.substringAfter("v=").substringBefore("&")
        }
        throw BusinessException(ChallengeVideoErrorCode.INVALID_VIDEO_FORMAT)
    }

    // ISO 8601 형식의 기간 문자열을 초 단위 정수로 변환하는 헬퍼 메서드
    private fun parseDuration(isoDuration: String): Int =
        Duration.parse(isoDuration).seconds.toInt()

    companion object {
        // YouTube Data API v3 공식 엔드포인트
        private const val YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/videos"
    }
}
