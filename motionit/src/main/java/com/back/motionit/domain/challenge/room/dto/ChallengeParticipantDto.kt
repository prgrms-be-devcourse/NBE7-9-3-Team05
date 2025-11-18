package com.back.motionit.domain.challenge.room.dto;

import java.time.LocalDateTime;

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole;

public record ChallengeParticipantDto(
	Long id,
	Long userId,
	LocalDateTime quitDate,
	Boolean quited,
	ChallengeParticipantRole role,
	Boolean challengeStatus
) {
	public ChallengeParticipantDto(ChallengeParticipant participant) {
		this(
			participant.getId(),
			participant.getUser().getId(),
			participant.getQuitDate(),
			participant.getQuited(),
			participant.getRole(),
			participant.getChallengeStatus()
		);
	}
}
