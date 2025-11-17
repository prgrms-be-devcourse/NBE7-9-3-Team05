package com.back.motionit.domain.challenge.mission.controller

import com.back.motionit.domain.challenge.mission.api.ChallengeMissionStatusApi
import com.back.motionit.domain.challenge.mission.api.response.ChallengeMissionStatusHttp
import com.back.motionit.domain.challenge.mission.dto.ChallengeMissionStatusResponse
import com.back.motionit.domain.challenge.mission.dto.ChallengeMissionStatusResponse.Companion.from
import com.back.motionit.domain.challenge.mission.service.ChallengeMissionStatusService
import com.back.motionit.domain.challenge.validator.ChallengeAuthValidator
import com.back.motionit.global.request.RequestContext
import com.back.motionit.global.respoonsedata.ResponseData
import com.back.motionit.global.respoonsedata.ResponseData.Companion.success
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/challenge/rooms/{roomId}/missions")
class ChallengeMissionStatusController(
    private val challengeMissionStatusService: ChallengeMissionStatusService,
    private val requestContext: RequestContext,
    private val challengeAuthValidator: ChallengeAuthValidator, // 챌린지 방참여자 여부 판단
) : ChallengeMissionStatusApi {
    private val log = KotlinLogging.logger {}

    @GetMapping("/ai-summary")
    override fun generateAiSummary(@PathVariable roomId: Long): ResponseData<String> {
        val actor = requestContext.getActor()
        val message = challengeMissionStatusService.generateAiSummary(roomId, actor.id!!)
        return success(
            "AI 응원 메시지 조회 완료",
            message
        )
    }

    @PostMapping("/complete")
    override fun completeMission(
        @PathVariable("roomId") roomId: Long
    ): ResponseData<ChallengeMissionStatusResponse> {
        val actor = requestContext.getActor()
        // 방 참여자가 아닐경우 API 접근 차단
        challengeAuthValidator.validateActiveParticipant(actor.id!!, roomId)

        val mission = challengeMissionStatusService.completeMission(
            roomId, actor.id!!
        )

        return success(
            ChallengeMissionStatusHttp.MISSION_COMPLETE_SUCCESS_MESSAGE,
            from(mission)
        )
    }

    @GetMapping("/today")
    override fun getTodayMissionByRoom(
        @PathVariable("roomId") roomId: Long
    ): ResponseData<List<ChallengeMissionStatusResponse>> {
        val actor = requestContext.getActor()
        // 방 참여자가 아닐경우 API 접근 차단
        challengeAuthValidator.validateActiveParticipant(actor.id!!, roomId)

        val list = challengeMissionStatusService
            .getTodayMissionsByRoom(roomId, actor.id!!)
            .map { from(it) }   // it은 ChallengeMissionStatus

        val message = if (list.isEmpty())
            ChallengeMissionStatusHttp.GET_TODAY_NO_MISSION_MESSAGE
        else
            ChallengeMissionStatusHttp.GET_TODAY_SUCCESS_MESSAGE

        return success(message, list)
    }

    @GetMapping("/personal/today")
    override fun getTodayMissionStatus(
        @PathVariable("roomId") roomId: Long
    ): ResponseData<ChallengeMissionStatusResponse> {
        val actor = requestContext.getActor()
        // 방 참여자가 아닐경우 API 접근 차단
        challengeAuthValidator.validateActiveParticipant(actor.id!!, roomId)

        val mission = challengeMissionStatusService.getTodayMissionStatus(roomId, actor.id!!)
        return success(
            ChallengeMissionStatusHttp.GET_TODAY_PARTICIPANT_SUCCESS_MESSAGE,
            from(mission)
        )
    }

    @GetMapping("/personal/history")
    override fun getMissionHistory(
        @PathVariable("roomId") roomId: Long
    ): ResponseData<List<ChallengeMissionStatusResponse>> {
        val actor = requestContext.getActor()
        // 방 참여자가 아닐경우 API 접근 차단
        challengeAuthValidator.validateActiveParticipant(actor.id!!, roomId)

        val list = challengeMissionStatusService
            .getMissionHistory(roomId, actor.id!!)
            .map { from(it) }

        return success(
            ChallengeMissionStatusHttp.GET_MISSION_HISTORY_SUCCESS_MESSAGE,
            list
        )
    }
}


// TODO: actor.id!! 부분 리팩토링