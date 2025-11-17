package com.back.motionit.domain.challenge.room.dto;

import com.back.motionit.global.enums.EventEnums;

public record RoomEventDto(
	String event
) {
	public RoomEventDto(EventEnums event) {
		this(event.getEvent());
	}
}
