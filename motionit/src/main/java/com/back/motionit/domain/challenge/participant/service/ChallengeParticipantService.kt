package com.back.motionit.domain.challenge.participant.service

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.room.dto.RoomEventDto
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.enums.EventEnums
import com.back.motionit.global.error.code.ChallengeParticipantErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.global.event.EventPublisher
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@RequiredArgsConstructor
class ChallengeParticipantService(
    private val challengeParticipantRepository: ChallengeParticipantRepository,
    private val challengeRoomRepository: ChallengeRoomRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: EventPublisher
) {

    @Transactional
    fun joinChallengeRoom(userId: Long, roomId: Long) {
        joinChallengeRoom(userId, roomId, ChallengeParticipantRole.NORMAL)
        eventPublisher.publishEvent(RoomEventDto(EventEnums.ROOM))
    }

    @Transactional
    fun joinChallengeRoom(userId: Long, roomId: Long, role: ChallengeParticipantRole) {
        val user = userRepository.findById(userId)
            .orElseThrow{ BusinessException(ChallengeParticipantErrorCode.NOT_FOUND_USER) }

        val challengeRoom = challengeRoomRepository.findByIdWithLock(roomId)
            .orElseThrow{ BusinessException(ChallengeParticipantErrorCode.CANNOT_FIND_CHALLENGE_ROOM) }

        // 이미 참여중인 사용자인지 확인
        // TODO: !!는 전환단계에서만 유지, user/room 전환 후 리팩터링 필요
        val alreadyJoined = challengeParticipantRepository
            .existsActiveParticipant(user.id!!, challengeRoom.id!!)
        if (alreadyJoined) {
            throw BusinessException(ChallengeParticipantErrorCode.ALREADY_JOINED)
        }

        // 챌린지 룸의 현재 참가자 수가 최대 인원 수에 도달했는지 확인
        val currentParticipants = challengeParticipantRepository.countByChallengeRoomAndQuitedFalse(challengeRoom)
        if (currentParticipants >= challengeRoom.capacity) {
            throw BusinessException(ChallengeParticipantErrorCode.FULL_JOINED_ROOM)
        }

        val participant = ChallengeParticipant(user, challengeRoom, role)
        challengeParticipantRepository.save(participant)
    }

    @Transactional
    fun leaveChallenge(userId: Long, challengeRoomId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow{ BusinessException(ChallengeParticipantErrorCode.NOT_FOUND_USER) }

        val challengeRoom = challengeRoomRepository.findById(challengeRoomId)
            .orElseThrow{ BusinessException(ChallengeParticipantErrorCode.CANNOT_FIND_CHALLENGE_ROOM) }

        val participant = challengeParticipantRepository.findByUserAndChallengeRoom(user, challengeRoom)
            ?: throw BusinessException(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM)

        // Soft delete
        participant.quitChallenge()
    }

    @Transactional(readOnly = true)
    fun isActiveParticipant(userId: Long, roomId: Long): Boolean {
        return challengeParticipantRepository.existsActiveParticipant(userId, roomId)
    }

    fun checkParticipantIsRoomHost(userId: Long, roomId: Long): Boolean {
        val user = userRepository.findById(userId)
            .orElseThrow{ BusinessException(ChallengeParticipantErrorCode.NOT_FOUND_USER) }

        val challengeRoom = challengeRoomRepository.findById(roomId)
            .orElseThrow{ BusinessException(ChallengeParticipantErrorCode.CANNOT_FIND_CHALLENGE_ROOM) }

        val participant = challengeParticipantRepository.findByUserAndChallengeRoom(user, challengeRoom)
            ?: throw BusinessException(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM)

        if (participant.role == ChallengeParticipantRole.HOST) {
            return true
        }

        return false
    }
}
