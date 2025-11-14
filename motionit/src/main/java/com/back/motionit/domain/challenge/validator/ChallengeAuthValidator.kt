package com.back.motionit.domain.challenge.validator

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.global.error.code.ChallengeParticipantErrorCode
import com.back.motionit.global.error.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class ChallengeAuthValidator(
    private val challengeParticipantRepository: ChallengeParticipantRepository
) {
    fun validateActiveParticipant(userId: Long, roomId: Long): ChallengeParticipant {
        return challengeParticipantRepository.findActiveParticipant(userId, roomId)
            ?: throw BusinessException(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM)
    }

    // join fetch로 room객체도 함께전달
    fun validateActiveParticipantWithRoom(userId: Long, roomId: Long): ChallengeParticipant {
        return challengeParticipantRepository.findActiveWithRoom(userId, roomId)
            ?: throw BusinessException(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM)
    }
}
