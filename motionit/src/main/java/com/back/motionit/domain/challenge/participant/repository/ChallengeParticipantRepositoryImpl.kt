package com.back.motionit.domain.challenge.participant.repository

import com.back.motionit.domain.challenge.participant.dto.JoinCheckDto
import com.back.motionit.domain.challenge.participant.entity.QChallengeParticipant
import com.back.motionit.domain.challenge.room.entity.QChallengeRoom
import com.querydsl.core.types.Projections
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class ChallengeParticipantRepositoryImpl(
    private val jpaQuery: JPAQueryFactory
): ChallengeParticipantCustom {
    override fun checkJoinStatus(
        userId: Long,
        roomId: Long
    ): JoinCheckDto {
        val cp = QChallengeParticipant.challengeParticipant

        val alreadyJoinedSub = JPAExpressions
            .select(cp.count())
            .from(cp)
            .where(
                cp.user.id.eq(userId),
                cp.challengeRoom.id.eq(roomId),
                cp.quited.isFalse
            )

        val currentCountSub = JPAExpressions
            .select(cp.count())
            .from(cp)
            .where(
                cp.challengeRoom.id.eq(roomId),
                cp.quited.isFalse
            )

        val result = jpaQuery
            .select(
                Projections.constructor(
                    JoinCheckDto::class.java,
                    alreadyJoinedSub,
                    currentCountSub
                )
            )
            .from(QChallengeRoom.challengeRoom) // dummy from
            .where(QChallengeRoom.challengeRoom.id.eq(roomId))
            .fetchOne()

        return result ?: JoinCheckDto(0, 0)
    }
}