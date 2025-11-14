package com.back.motionit.domain.challenge.validator;

import org.springframework.stereotype.Component;

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;
import com.back.motionit.global.error.code.ChallengeParticipantErrorCode;
import com.back.motionit.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChallengeAuthValidator {

	private final ChallengeParticipantRepository challengeParticipantRepository;

	public ChallengeParticipant validateActiveParticipant(Long userId, Long roomId) {
		return challengeParticipantRepository.findActiveParticipant(userId, roomId)
			.orElseThrow(() -> new BusinessException(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM));
	}

	// join fetch로 room객체도 함께전달
	public ChallengeParticipant validateActiveParticipantWithRoom(Long userId, Long roomId) {
		return challengeParticipantRepository.findActiveWithRoom(userId, roomId)
			.orElseThrow(() -> new BusinessException(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM));
	}
}
