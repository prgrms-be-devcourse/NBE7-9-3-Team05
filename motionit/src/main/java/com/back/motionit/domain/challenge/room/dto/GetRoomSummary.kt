package com.back.motionit.domain.challenge.room.dto;

import com.back.motionit.global.enums.ChallengeStatus;

public record GetRoomSummary(
	Long id,
	String title,
	String description,
	Integer capacity,
	Integer dDay,
	String roomImage,
	ChallengeStatus status,
	Integer current
) {
}
