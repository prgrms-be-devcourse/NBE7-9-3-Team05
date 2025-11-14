package com.back.motionit.domain.challenge.room.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.video.entity.OpenStatus;

public record GetRoomResponse(
	Long id,
	String title,
	String description,
	int capacity,
	OpenStatus openStatus,
	LocalDateTime challengeStartDate,
	LocalDateTime challengeEndDate,
	String roomImage,
	List<ChallengeVideoDto> videos,
	List<ChallengeParticipantDto> participants
) {
	public GetRoomResponse(
		ChallengeRoom room,
		List<ChallengeVideoDto> videos,
		List<ChallengeParticipantDto> participants
	) {
		this(
			room.getId(),
			room.getTitle(),
			room.getDescription(),
			room.getCapacity(),
			room.getOpenStatus(),
			room.getChallengeStartDate(),
			room.getChallengeEndDate(),
			room.getRoomImage(),
			videos,
			participants
		);
	}
}
