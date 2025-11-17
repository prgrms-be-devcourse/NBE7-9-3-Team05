package com.back.motionit.domain.challenge.mission.api

import com.back.motionit.domain.challenge.mission.api.response.ChallengeMissionStatusHttp
import com.back.motionit.domain.challenge.mission.dto.ChallengeMissionStatusResponse
import com.back.motionit.global.respoonsedata.ResponseData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping

@Tag(
    name = "ApiV1ChallengeMissionStatusController",
    description = "운동방 미션 상태 조회 및 관리 API"
)
interface ChallengeMissionStatusApi {

    @GetMapping("/api/v1/challenge/rooms/{roomId}/missions/ai-summary")
    @Operation(
        summary = "AI 응원메세지 받아오기",
        description = "AI가 생성한 응원 메시지를 반환합니다."
    )
    fun generateAiSummary(
        @PathVariable("roomId") roomId: Long
    ): ResponseData<String>


    @PostMapping("/api/v1/challenge/rooms/{roomId}/missions/complete")
    @Operation(
        summary = "미션 완료 처리",
        description = "참여자가 미션을 완료했음을 처리합니다.",
        responses = [
            ApiResponse(
                responseCode = ChallengeMissionStatusHttp.MISSION_COMPLETE_SUCCESS_CODE,
                description = ChallengeMissionStatusHttp.MISSION_COMPLETE_SUCCESS_MESSAGE
            ),
            ApiResponse(
                responseCode = "400",
                description = "존재하지 않는 유저 또는 방 / 이미 완료된 미션"
            )
        ]
    )
    fun completeMission(
        @PathVariable("roomId") roomId: Long
    ): ResponseData<ChallengeMissionStatusResponse>


    @GetMapping("/api/v1/challenge/rooms/{roomId}/missions/today")
    @Operation(
        summary = "오늘의 운동방 미션 현황 조회",
        description = """
            해당 운동방의 오늘 미션 현황을 조회합니다.
            데이터가 없는 경우에도 200 OK와 빈 배열([])을 반환합니다.
        """,
        responses = [
            ApiResponse(
                responseCode = ChallengeMissionStatusHttp.GET_TODAY_SUCCESS_CODE,
                description = ChallengeMissionStatusHttp.GET_TODAY_SUCCESS_MESSAGE
            ),
            ApiResponse(responseCode = "400", description = "존재하지 않는 운동방")
        ]
    )
    fun getTodayMissionByRoom(
        @PathVariable("roomId") roomId: Long
    ): ResponseData<List<ChallengeMissionStatusResponse>>


    @GetMapping("/api/v1/challenge/rooms/{roomId}/missions/personal/today")
    @Operation(
        summary = "오늘의 참여자 미션 상태 조회",
        description = "해당 참여자의 오늘 미션 상태를 조회합니다.",
        responses = [
            ApiResponse(
                responseCode = ChallengeMissionStatusHttp.GET_TODAY_PARTICIPANT_SUCCESS_CODE,
                description = ChallengeMissionStatusHttp.GET_TODAY_PARTICIPANT_SUCCESS_MESSAGE
            ),
            ApiResponse(responseCode = "400", description = "존재하지 않는 유저 또는 방")
        ]
    )
    fun getTodayMissionStatus(
        @PathVariable("roomId") roomId: Long
    ): ResponseData<ChallengeMissionStatusResponse>


    @GetMapping("/api/v1/challenge/rooms/{roomId}/missions/personal/history")
    @Operation(
        summary = "참여자 미션 이력 조회",
        description = "해당 참여자의 미션 이력을 조회합니다.",
        responses = [
            ApiResponse(
                responseCode = ChallengeMissionStatusHttp.GET_MISSION_HISTORY_SUCCESS_CODE,
                description = ChallengeMissionStatusHttp.GET_MISSION_HISTORY_SUCCESS_MESSAGE
            ),
            ApiResponse(responseCode = "400", description = "존재하지 않는 유저 또는 방")
        ]
    )
    fun getMissionHistory(
        @PathVariable("roomId") roomId: Long
    ): ResponseData<List<ChallengeMissionStatusResponse>>
}