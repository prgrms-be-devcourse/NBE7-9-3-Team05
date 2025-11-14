package com.back.motionit.domain.challenge.mission.service

import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus
import com.back.motionit.domain.challenge.mission.repository.ChallengeMissionStatusRepository
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.validator.ChallengeAuthValidator
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository
import com.back.motionit.global.error.code.ChallengeMissionErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.global.service.GptService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ChallengeMissionStatusService(
    private val challengeMissionStatusRepository: ChallengeMissionStatusRepository,
    private val challengeParticipantRepository: ChallengeParticipantRepository,
    private val challengeVideoRepository: ChallengeVideoRepository,
    private val challengeAuthValidator: ChallengeAuthValidator,
    private val gptProvider: ObjectProvider<GptService>,
) {
    private val log = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    fun generateAiSummary(roomId: Long, actorId: Long): String {
        val participant = challengeAuthValidator.validateActiveParticipant(actorId, roomId)
        val mission = getTodayMissionStatus(roomId, actorId)
        val gptService = gptProvider.getIfAvailable()

        // 이미 저장된 AI 메시지가 있으면 그대로 반환
        mission.aiMessage?.let{
            if(it.isNotBlank()){
                log.info { "[AI Summary] Using cached message user=$actorId room=$roomId" }
                return it
            }
        }

        if(gptService == null){
            log.warn {"[AI Summary] GPT disabled or not configured. user=$actorId room=$roomId" }
            return "응원 메시지를 준비중입니다" // 테스트/비활성 환경 기본 응답
        }

        return runCatching {
            gptService.generateMissionCompleteSummary(
                    participant.user.nickname,
                    participant.challengeRoom.title
                    )
        }.getOrElse{
            log.error(it){"[AI Summary] failed to generate summary for user=$actorId room=$roomId"}
            "응원 메시지 생성에 실패했습니다"
        }
    }

    @Transactional
    fun completeMission(roomId: Long, actorId: Long): ChallengeMissionStatus {
        // 참여중인 참가자인지 확인 - controller를 거치지 않은 호출에 대비
        val participant = challengeAuthValidator.validateActiveParticipant(actorId, roomId)
        val gptService = gptProvider.ifAvailable

        val today = LocalDate.now()

        // 영상 존재 확인
        if (!challengeVideoRepository.existsByChallengeRoomIdAndUploadDate(roomId, today)) {
            throw BusinessException(ChallengeMissionErrorCode.NO_VIDEO_UPLOADED)
        }

        val mission = challengeMissionStatusRepository
            .findByParticipantIdAndMissionDate(participant.id!!, today)
            ?: challengeMissionStatusRepository.save(
                ChallengeMissionStatus.create(participant, today)
            )

        // 이미 완료된 미션인지 확인
        if (mission.completed) {
            throw BusinessException(ChallengeMissionErrorCode.ALREADY_COMPLETED)
        }

        // 미션 완료 상태로 업데이트
        mission.completeMission()

        if(mission.aiMessage.isNullOrEmpty() && gptService != null){
            val aiMessage = runCatching {
                gptService.generateMissionCompleteSummary(
                    participant.user.nickname,
                    participant.challengeRoom.title
                )
            }.getOrElse {
                log.error(it) {"[Mission Complete] Failed to generate AI message for user={}, room={}"}
                "응원 메시지 생성에 실패했습니다"
            }

            mission.updateAiMessage(aiMessage)
        }

        return mission
    }

    @Transactional(readOnly = true)
    fun getTodayMissionStatus(roomId: Long, actorId: Long): ChallengeMissionStatus {
        val participant = challengeAuthValidator.validateActiveParticipant(actorId, roomId)
        val today = LocalDate.now()

        return challengeMissionStatusRepository
            .findByParticipantIdAndMissionDate(participant.id!!, today)
            ?: throw BusinessException(ChallengeMissionErrorCode.NOT_INITIALIZED_MISSION)
    }

    // 특정 운동방의 모든 참가자의 오늘 미션 상태 조회
    @Transactional(readOnly = true)
    fun getTodayMissionsByRoom(roomId: Long, actorId: Long): List<ChallengeMissionStatus> {
        // 접근 권한 확인
        val participant = challengeAuthValidator.validateActiveParticipant(actorId, roomId)
        val room = participant.challengeRoom
        val today = LocalDate.now()

        // TODO: 쿼리 최적화 필요 (LEFT JOIN)
        val participants =
            challengeParticipantRepository.findAllByChallengeRoomAndQuitedFalse(room)

        // 오늘 미션 완료자 조회 (참가자까지 fetch)
        val missions =
            challengeMissionStatusRepository.findByRoomAndDate(room, today)

        // participantId → mission 매핑
        val missionMap = missions.associateBy { it.participant.id!! }

        // 전체 참가자 기준 병합 (미완료자 포함)
        val allStatuses = participants.map { p ->
            missionMap[p.id] ?: ChallengeMissionStatus.create(p, today)
        }

        if (allStatuses.isEmpty()) {
            log.warn { "[getTodayMissionsByRoom] $today 날짜에 미션 데이터가 없습니다. (roomId=$roomId)" }
        }
        return allStatuses
    }

    // 참가자의 미션 수행 내역 조회
    @Transactional(readOnly = true)
    fun getMissionHistory(roomId: Long, actorId: Long): List<ChallengeMissionStatus> {
        val participant = challengeAuthValidator.validateActiveParticipant(actorId, roomId)
        return challengeMissionStatusRepository.findAllByParticipantId(participant.id!!)
    }
}
// TODO: participant.id!! -> baseEntity 구조문제로 추후 !! 제거