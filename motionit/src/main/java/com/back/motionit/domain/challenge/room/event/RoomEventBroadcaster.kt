package com.back.motionit.domain.challenge.room.event;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.back.motionit.domain.challenge.room.dto.RoomEventDto;
import com.back.motionit.global.event.Broadcaster;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoomEventBroadcaster implements Broadcaster<RoomEventDto> {

	private final SimpMessagingTemplate messagingTemplate;

	@Override
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCreated(RoomEventDto event) {
		messagingTemplate.convertAndSend("/topic/challenge/rooms", event);
	}
}
