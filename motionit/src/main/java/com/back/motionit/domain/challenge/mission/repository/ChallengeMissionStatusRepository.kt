package com.back.motionit.domain.challenge.mission.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;

public interface ChallengeMissionStatusRepository extends JpaRepository<ChallengeMissionStatus, Long> {
	Optional<ChallengeMissionStatus> findByParticipantIdAndMissionDate(Long participantId, LocalDate today);

	boolean existsByParticipantIdAndMissionDate(Long id, LocalDate today);

	List<ChallengeMissionStatus> findAllByParticipantId(Long participantId);

	/**
	 * 특정 운동방의 특정 날짜의 모든 참가자들의 미션 상태들을 조회합니다.
	 */
	@Query("""
		SELECT ms
		FROM ChallengeMissionStatus ms
		JOIN ms.participant p
		WHERE p.challengeRoom = :room
		AND ms.missionDate = :missionDate
		""")
	List<ChallengeMissionStatus> findByRoomAndDate(
		@Param("room") ChallengeRoom room,
		@Param("missionDate") LocalDate missionDate
	);
}
