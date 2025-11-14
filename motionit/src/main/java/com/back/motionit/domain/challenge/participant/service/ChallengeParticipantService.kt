package com.back.motionit.domain.challenge.participant.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole;
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;
import com.back.motionit.domain.challenge.room.dto.RoomEventDto;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.enums.EventEnums;
import com.back.motionit.global.error.code.ChallengeParticipantErrorCode;
import com.back.motionit.global.error.exception.BusinessException;
import com.back.motionit.global.event.EventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeParticipantService {

	private final ChallengeParticipantRepository challengeParticipantRepository;
	private final ChallengeRoomRepository challengeRoomRepository;
	private final UserRepository userRepository;
	private final EventPublisher eventPublisher;

	@Transactional
	public void joinChallengeRoom(Long userId, Long roomId) {
		joinChallengeRoom(userId, roomId, ChallengeParticipantRole.NORMAL);
		eventPublisher.publishEvent(new RoomEventDto(EventEnums.ROOM));
	}

	@Transactional
	public void joinChallengeRoom(Long userId, Long roomId, ChallengeParticipantRole role) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ChallengeParticipantErrorCode.NOT_FOUND_USER));

		ChallengeRoom challengeRoom = challengeRoomRepository.findByIdWithLock(roomId)
			.orElseThrow(() -> new BusinessException(ChallengeParticipantErrorCode.CANNOT_FIND_CHALLENGE_ROOM));

		// 이미 참여중인 사용자인지 확인
		boolean alreadyJoined = challengeParticipantRepository.existsActiveParticipant(user.getId(),
			challengeRoom.getId());
		if (alreadyJoined) {
			throw new BusinessException(ChallengeParticipantErrorCode.ALREADY_JOINED);
		}

		// 챌린지 룸의 현재 참가자 수가 최대 인원 수에 도달했는지 확인
		Integer currentParticipants = challengeParticipantRepository.countByChallengeRoomAndQuitedFalse(challengeRoom);
		if (currentParticipants >= challengeRoom.getCapacity()) {
			throw new BusinessException(ChallengeParticipantErrorCode.FULL_JOINED_ROOM);
		}

		ChallengeParticipant participant = new ChallengeParticipant(user, challengeRoom, role);
		challengeParticipantRepository.save(participant);
	}

	@Transactional
	public void leaveChallenge(Long userId, Long challengeRoomId) {

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ChallengeParticipantErrorCode.NOT_FOUND_USER));

		ChallengeRoom challengeRoom = challengeRoomRepository.findById(challengeRoomId)
			.orElseThrow(() -> new BusinessException(ChallengeParticipantErrorCode.CANNOT_FIND_CHALLENGE_ROOM));

		ChallengeParticipant participant = challengeParticipantRepository.findByUserAndChallengeRoom(user, challengeRoom);

		if (participant == null) {
			throw new BusinessException(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM);
		}

		// Soft delete
		participant.quitChallenge();
	}

	@Transactional(readOnly = true)
	public boolean isActiveParticipant(Long userId, Long roomId) {
		return challengeParticipantRepository.existsActiveParticipant(userId, roomId);
	}

	public boolean checkParticipantIsRoomHost(Long userId, Long roomId) {

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ChallengeParticipantErrorCode.NOT_FOUND_USER));

		ChallengeRoom challengeRoom = challengeRoomRepository.findById(roomId)
			.orElseThrow(() -> new BusinessException(ChallengeParticipantErrorCode.CANNOT_FIND_CHALLENGE_ROOM));

		ChallengeParticipant participant = challengeParticipantRepository.findByUserAndChallengeRoom(user, challengeRoom);

		if (participant == null) {
			throw new BusinessException(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM);
		}

		if (participant.getRole().equals(ChallengeParticipantRole.HOST)) {
			return true;
		}

		return false;
	}
}
