package com.back.motionit.domain.challenge.mission.dto;

import java.time.LocalDate;

import org.springframework.lang.Nullable;

import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole;

public record ChallengeMissionStatusResponse(
	Long participantId,
	String nickname,
	String userProfile,
	LocalDate missionDate,
	boolean completed,
	ChallengeParticipantRole isHost,
	@Nullable String aiSummary
) {
	public static ChallengeMissionStatusResponse from(ChallengeMissionStatus status) {
		var participant = status.getParticipant();
		var user = participant.getUser(); // TODO: 지연로딩으로 N+1 발생가능지점, 쿼리개선 필요

		return new ChallengeMissionStatusResponse(
			status.getParticipant().getId(),
			user.getNickname(),
			user.getUserProfile(),
			status.getMissionDate(),
			status.getCompleted(),
			status.getParticipant().getRole(),
			status.getAiMessage()
		);
	}

	public static ChallengeMissionStatusResponse from(ChallengeMissionStatus status, String aiSummary) {
		var participant = status.getParticipant();
		var user = participant.getUser();
		return new ChallengeMissionStatusResponse(
			status.getParticipant().getId(),
			user.getNickname(),
			user.getUserProfile(),
			status.getMissionDate(),
			status.getCompleted(),
			status.getParticipant().getRole(),
			aiSummary
		);
	}
}
