package com.back.motionit.global.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus;
import com.back.motionit.domain.challenge.mission.repository.ChallengeMissionStatusRepository;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeMissionScheduler {

	private final ChallengeMissionStatusRepository challengeMissionStatusRepository;
	private final ChallengeParticipantRepository challengeParticipantRepository;

	@Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
	@Transactional
	public void initializeChallengeDailyMissions() {
		LocalDate today = LocalDate.now();
		log.info("[Scheduler] {} 운동방 미션 초기화 시작", today);

		List<ChallengeParticipant> participants = challengeParticipantRepository.findAll();
		for (ChallengeParticipant participant : participants) {
			try {
				boolean exists = challengeMissionStatusRepository.existsByParticipantIdAndMissionDate(
					participant.getId(), today);
				if (!exists) {
					challengeMissionStatusRepository.save(ChallengeMissionStatus.create(participant, today));
				}
			} catch (DataIntegrityViolationException e) {
				log.warn("Duplicate mission ignored: participant={} date={}", participant.getId(), today);
			}
		}

		log.info("[Scheduler] {} 날짜 미션 초기화 완료 ({}명)", today, participants.size());
	}
}
