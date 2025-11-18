package com.back.motionit.domain.challenge.participant.dto

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant

data class ChallengeParticipantResponse(
    val userId: Long,
    val roomId: Long,
    val joined: Boolean
) {
    companion object {
        fun from(participant: ChallengeParticipant): ChallengeParticipantResponse {
            return ChallengeParticipantResponse(
                participant.user.id!!,
                participant.challengeRoom.id!!,
                !participant.quited
            )
        }
    }
}
