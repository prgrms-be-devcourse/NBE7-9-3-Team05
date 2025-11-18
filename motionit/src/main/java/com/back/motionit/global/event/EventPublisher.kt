package com.back.motionit.global.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class EventPublisher(
    private val publisher: ApplicationEventPublisher
) {

    fun publishEvent(event: Any) {
        publisher.publishEvent(event)
    }
}
