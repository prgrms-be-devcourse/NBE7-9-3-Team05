package com.back.motionit.global.outbox.processor

import com.back.motionit.domain.challenge.video.dto.YoutubeVideoPayload
import com.back.motionit.domain.challenge.video.external.youtube.YoutubeMetadataResilientClient
import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata
import com.back.motionit.domain.challenge.video.service.ChallengeVideoService
import com.back.motionit.global.enums.EventEnums
import com.back.motionit.global.enums.OutboxEventType
import com.back.motionit.global.enums.OutboxStatus
import com.back.motionit.global.event.EventPublisher
import com.back.motionit.global.outbox.entity.OutboxEvent
import com.back.motionit.global.outbox.repository.OutboxEventRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OutboxEventHandler(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
    private val videoService: ChallengeVideoService,
    private val youtubeMetadataResilientClient: YoutubeMetadataResilientClient,
    private val eventPublisher: EventPublisher,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun processSingleEvent(event: OutboxEvent) {
        // 이미 처리 중인 상태면 무시, 동시성 대비
        if (event.status != OutboxStatus.PENDING) {
            return
        }

        event.status = OutboxStatus.PROCESSING
        event.lastTriedAt = LocalDateTime.now()
        event.attemptCount += 1
        outboxEventRepository.save(event)

        try {
            when (event.eventType) {
                OutboxEventType.YOUTUBE_VIDEO -> handleVideoUploaded(event)
            }

            event.status = OutboxStatus.COMPLETED
            event.lastErrorMessage = null
        } catch (ex: CallNotPermittedException) {
            // 서킷 OPEN -> 외부 시스템이 죽은 상태
            // attemptCount 가 증가되지 않음
            log.warn {
                "Circuit OPEN for youtubeMetadata, eventId=${event.id} - will retry later"
            }

            event.status = OutboxStatus.PENDING
            event.lastErrorMessage = "CircuitOpen: ${ex.message}".take(490)
        } catch (ex: Exception) {
            log.error(ex) {
                "OutboxProcessor = error while processing eventId=${event.id}, type=${event.eventType}, attempt=${event.attemptCount}"
            }

            event.status = if (event.attemptCount >= 5) {
                OutboxStatus.FAILED
            } else {
                OutboxStatus.PENDING
            }

            event.lastErrorMessage = ex.message?.take(490)
        } finally {
            outboxEventRepository.save(event)
        }
    }

    private fun handleVideoUploaded(event: OutboxEvent) {
        val payload = objectMapper.readValue(
            event.payload,
            YoutubeVideoPayload::class.java
        )

        val metadata: YoutubeVideoMetadata =
            youtubeMetadataResilientClient.fetchMetadata(payload.youtubeUrl)

        videoService.saveChallengeVideo(
            actorId = payload.userId,
            roomId = payload.roomId,
            metadata = metadata,
        )

        try {
            eventPublisher.publishEvent(EventEnums.ROOM)
        } catch (ex: Exception) {
            log.error { "Outbox Youtube Video Socket Event Publish Error: ${ex}" }
        }
    }
}
