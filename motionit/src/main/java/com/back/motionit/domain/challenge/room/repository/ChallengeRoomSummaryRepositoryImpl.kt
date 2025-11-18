package com.back.motionit.domain.challenge.room.repository

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.video.entity.OpenStatus
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class ChallengeRoomSummaryRepositoryImpl(
    private val manager: EntityManager
) : ChallengeRoomSummaryRepository {

    override fun fetchOpenRooms(pageable: Pageable): Page<ChallengeRoom> {
        val data = manager.createQuery(
            """
					select r
					from ChallengeRoom r
					where r.openStatus = :status
					order by r.createDate desc
				
				""".trimIndent(), ChallengeRoom::class.java
        )
            .setParameter("status", OpenStatus.OPEN)
            .setFirstResult(pageable.offset.toInt())
            .setMaxResults(pageable.pageSize)
            .resultList

        val total = countOpenRooms().toLong()
        return PageImpl(data, pageable, total)
    }

    override fun countOpenRooms(): Int {
        val cnt = manager.createQuery(
            """
				select count(r)
				from ChallengeRoom r
				where r.openStatus = :status
			
			""".trimIndent(), Long::class.java
        )
            .setParameter("status", OpenStatus.OPEN)
            .singleResult
        return cnt.toInt()
    }
}
