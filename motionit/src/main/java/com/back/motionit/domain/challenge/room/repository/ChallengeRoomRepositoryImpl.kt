package com.back.motionit.domain.challenge.room.repository

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.entity.QChallengeRoom
import com.back.motionit.domain.challenge.video.entity.QChallengeVideo
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class ChallengeRoomRepositoryImpl(
    private val jpaQuery: JPAQueryFactory,
) : ChallengeRoomRepositoryCustom {
    override fun findDetailById(id: Long): ChallengeRoom? {
        val challengeRoom = QChallengeRoom.challengeRoom
        val challengeVideo = QChallengeVideo.challengeVideo

        return jpaQuery
            .selectFrom(challengeRoom).distinct()
            .leftJoin(challengeRoom.challengeVideoList, challengeVideo).fetchJoin()
            .where(challengeRoom.id.eq(id))
            .fetchOne()
    }
}
