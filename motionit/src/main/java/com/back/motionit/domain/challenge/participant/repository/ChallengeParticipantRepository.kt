package com.back.motionit.domain.challenge.participant.repository

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChallengeParticipantRepository : JpaRepository<ChallengeParticipant, Long>, ChallengeParticipantCustom {
    fun existsByUserAndChallengeRoom(user: User, challengeRoom: ChallengeRoom): Boolean

    fun findByUserAndChallengeRoom(user: User, challengeRoom: ChallengeRoom): ChallengeParticipant?

    fun countByChallengeRoomAndQuitedFalse(challengeRoom: ChallengeRoom): Int

    fun findAllByChallengeRoomAndQuitedFalse(room: ChallengeRoom): List<ChallengeParticipant>

    @Query(
        """
			select case when count(p) > 0 then true else false end
			from ChallengeParticipant p
			where p.user.id = :userId
			and p.challengeRoom.id = :roomId
			and p.quited = false
		"""
    )
    fun existsActiveParticipant(
        @Param("userId") userId: Long,
        @Param("roomId") roomId: Long
    ): Boolean

    @Query(
        """
			select p from ChallengeParticipant p
			join fetch p.user
			where p.challengeRoom.id = :roomId
		"""
    )
    fun findAllByRoomIdWithUser(@Param("roomId") roomId: Long): List<ChallengeParticipant>

    @Query(
        """
			select p from ChallengeParticipant p
			where p.user.id = :userId
			and p.challengeRoom.id = :roomId
			and p.quited = false
		"""
    )
    fun findActiveParticipant(
        @Param("userId") userId: Long,
        @Param("roomId") roomId: Long
    ): ChallengeParticipant?

    @Query(
        """
			select cp
			from ChallengeParticipant cp
			join fetch cp.challengeRoom cr
			where cp.user.id = :userId
			and cr.id = :roomId
			and cp.quited = false
		"""
    )
    fun findActiveWithRoom(
        @Param("userId") userId: Long,
        @Param("roomId") roomId: Long
    ): ChallengeParticipant?

    // 사용자 참여중 방 id 리스트(현재 페이지 roomIds 한정)
    @Query(
        """
		select cp.challengeRoom.id
		from ChallengeParticipant cp
		where cp.quited = false
			and cp.user.id = :userId
			and cp.challengeRoom.id in :roomIds
		"""
    )
    fun findJoiningRoomIdsByUserAndRoomIds(
        @Param("userId") userId: Long,
        @Param("roomIds") roomIds: Collection<Long>
    ): List<Long>

    // roomId별 현재 인원수(quited=false) 집계
    @Query(
        """
		select cp.challengeRoom.id as roomId, count(cp.id) as cnt
		from ChallengeParticipant cp
		where cp.quited = false
			and cp.challengeRoom.id in :roomIds
		group by cp.challengeRoom.id
		"""
    )
    fun countActiveParticipantsByRoomIds(
        @Param("roomIds") roomIds: Collection<Long>
    ): MutableList<Array<Any>>
}
