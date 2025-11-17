package com.back.motionit.domain.challenge.mission.repository

import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ChallengeMissionStatusRepository : JpaRepository<ChallengeMissionStatus, Long> {
    fun findByParticipantIdAndMissionDate(participantId: Long, today: LocalDate): ChallengeMissionStatus?

    fun existsByParticipantIdAndMissionDate(id: Long, today: LocalDate): Boolean

    fun findAllByParticipantId(participantId: Long): List<ChallengeMissionStatus>

    /**
     * 특정 운동방의 특정 날짜의 모든 참가자들의 미션 상태들을 조회합니다.
     */
    @Query(
        """
		SELECT ms
		FROM ChallengeMissionStatus ms
		JOIN ms.participant p
		WHERE p.challengeRoom = :room
		AND ms.missionDate = :missionDate
		"""
    )
    fun findByRoomAndDate(
        @Param("room") room: ChallengeRoom,
        @Param("missionDate") missionDate: LocalDate
    ): List<ChallengeMissionStatus>
}
