package com.back.motionit.domain.challenge.video.external.youtube

import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component

@Component
class YoutubeMetadataResilientClient(
    private val delegate: YoutubeMetadataClient,
) {
    @CircuitBreaker(name = "youtubeMetadata")
    @Retry(name = "youtubeMetadata")
    @RateLimiter(name = "youtubeMetadata")
    fun fetchMetadata(youtubeUrl: String) : YoutubeVideoMetadata {
        return delegate.fetchMetadata(youtubeUrl)
    }
}
