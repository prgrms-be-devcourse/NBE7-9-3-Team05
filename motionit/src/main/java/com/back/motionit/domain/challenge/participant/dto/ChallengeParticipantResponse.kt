package com.back.motionit.domain.challenge.participant.dto;

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;

public record ChallengeParticipantResponse(
	Long userId,
	Long roomId,
	boolean joined
) {
	public static ChallengeParticipantResponse from(ChallengeParticipant participant) {
		return new ChallengeParticipantResponse(
			participant.getUser().getId(),
			participant.getChallengeRoom().getId(),
			!participant.getQuited()
		);
	}
}
