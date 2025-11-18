package com.back.motionit.global.outbox.processor

import com.back.motionit.global.enums.OutboxStatus
import com.back.motionit.global.outbox.repository.OutboxEventRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OutboxProcessor(
    private val outboxEventRepository: OutboxEventRepository,
    private val outboxEventHandler: OutboxEventHandler,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(fixedDelay = 5_000)
    fun pollAndProcess() {
        val events = outboxEventRepository.findTop100ByStatusOrderById(OutboxStatus.PENDING)

        if (events.isNotEmpty()) {
            log.debug { "OutboxProcessor - processing ${events.size} pending events" }
        }

        events.forEach { event ->
            try {
                outboxEventHandler.processSingleEvent(event)
            } catch (ex: Exception) {
                log.error(ex) { "OutboxProcessor - unexpected error while processing eventId=${event.id}" }
            }
        }
    }
}
