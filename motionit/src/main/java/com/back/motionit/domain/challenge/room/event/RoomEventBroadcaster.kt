package com.back.motionit.domain.challenge.room.event

import com.back.motionit.domain.challenge.room.dto.RoomEventDto
import com.back.motionit.global.event.Broadcaster
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class RoomEventBroadcaster(
    private val messagingTemplate: SimpMessagingTemplate
) : Broadcaster<RoomEventDto> {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    override fun onCreated(event: RoomEventDto) {
        messagingTemplate.convertAndSend("/topic/challenge/rooms", event)
    }
}
