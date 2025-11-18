package com.back.motionit.global.scheduler

import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus.Companion.create
import com.back.motionit.domain.challenge.mission.repository.ChallengeMissionStatusRepository
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class ChallengeMissionScheduler(
    private val challengeMissionStatusRepository: ChallengeMissionStatusRepository,
    private val challengeParticipantRepository: ChallengeParticipantRepository,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    @Transactional
    fun initializeChallengeDailyMissions() {
        val today = LocalDate.now()
        log.info { "[Scheduler] $today 운동방 미션 초기화 시작" }

        val participants = challengeParticipantRepository.findAll()

        participants.forEach { participant ->
            val participantId = participant.id ?: return@forEach

            runCatching {
                val exists = challengeMissionStatusRepository.existsByParticipantIdAndMissionDate(
                    participantId, today
                )

                if (!exists) {
                    challengeMissionStatusRepository.save(create(participant, today))
                }
            }.onFailure { e ->
                when (e) {
                    is DataIntegrityViolationException -> {
                        log.warn(e) {
                            "Duplicate mission ignored: participant=$participantId date=$today"
                        }
                    }
                    else -> throw e
                }
            }
        }

        log.info { "[Scheduler] $today 날짜 미션 초기화 완료 (${participants.size}명)" }
    }
}
