package com.back.motionit.domain.challenge.participant.repository

import com.back.motionit.domain.challenge.participant.dto.JoinCheckDto
import com.back.motionit.domain.challenge.participant.entity.QChallengeParticipant

class ChallengeParticipantRepositoryImpl(
    private val jpaQuery: JPAQueryFactory
): ChallengeParticipantCustom {
    override fun checkJoinStatus(
        userId: Long,
        roomId: Long
    ): JoinCheckDto {
        val participant = QChallengeParticipant.challengeParticipant

        val alreadyJoinedQuery = jpaQuery
            .select(participant.count())
            .from(participant)
            .where(
                participant.user.id.eq(userId),
                participant.challengeRoom.id.eq(roomId),
                participant.quited.isFalse
            )


    }
}