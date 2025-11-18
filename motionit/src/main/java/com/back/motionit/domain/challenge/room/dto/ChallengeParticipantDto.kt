package com.back.motionit.domain.challenge.room.dto

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole
import java.time.LocalDateTime

data class ChallengeParticipantDto(
    val id: Long?,
    val userId: Long?,
    val quitDate: LocalDateTime?,
    val quited: Boolean,
    val role: ChallengeParticipantRole,
    val challengeStatus: Boolean
) {
    constructor(participant: ChallengeParticipant) : this(
        participant.id,
        participant.user.id,
        participant.quitDate,
        participant.quited,
        participant.role,
        participant.challengeStatus
    )
}
