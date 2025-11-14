package com.back.motionit.domain.challenge.video.external.youtube

import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("perf", "test") // perf, test 프로필에서만 활성화
class YoutubeMetadataClientStub(
    @Value("\${youtube.api.key:dummy-key}")
    private val apiKey: String
) : YoutubeMetadataClient(apiKey) {

    override fun fetchMetadata(youtubeUrl: String): YoutubeVideoMetadata {
        val videoId = extractVideoId(youtubeUrl)

        return YoutubeVideoMetadata(
            videoId = videoId,
            title = "Performance Test Video - $videoId",
            thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
            durationSeconds = 120
        )
    }
}