package com.back.motionit.global.scheduler

import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus
import com.back.motionit.domain.challenge.mission.repository.ChallengeMissionStatusRepository
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class ChallengeMissionScheduler(
    private val challengeMissionStatusRepository: ChallengeMissionStatusRepository,
    private val challengeParticipantRepository: ChallengeParticipantRepository
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    @Transactional
    fun initializeChallengeDailyMissions() {
        val today = LocalDate.now()
        log.info { "[Scheduler] $today 운동방 미션 초기화 시작" }

        val participants = challengeParticipantRepository.findAll()

        participants.forEach { participant ->
            try {
                val exists = challengeMissionStatusRepository
                    .existsByParticipantIdAndMissionDate(participant.id!!, today)

                if (!exists) {
                    challengeMissionStatusRepository.save(
                        ChallengeMissionStatus.create(participant, today)
                    )
                }

            } catch (e: Exception) {
                log.warn { "Duplicate mission ignored: participant=${participant.id} date=$today" }
            }
        }

        log.info { "[Scheduler] $today 날짜 미션 초기화 완료 (${participants.size}명)" }
    }
}
