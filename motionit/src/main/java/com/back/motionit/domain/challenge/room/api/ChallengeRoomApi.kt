package com.back.motionit.domain.challenge.room.api

import com.back.motionit.domain.challenge.room.api.response.ChallengeRoomHttp
import com.back.motionit.domain.challenge.room.dto.CreateRoomRequest
import com.back.motionit.domain.challenge.room.dto.CreateRoomResponse
import com.back.motionit.domain.challenge.room.dto.GetRoomResponse
import com.back.motionit.domain.challenge.room.dto.GetRoomsResponse
import com.back.motionit.global.constants.ChallengeRoomConstants
import com.back.motionit.global.respoonsedata.ResponseData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.web.bind.annotation.*

@Tag(name = "운동방", description = "Controller to handle challenge room API")
interface ChallengeRoomApi {
    @PostMapping
    @Operation(summary = "Create Challenge Room", description = "운동방 생성 요청값을 전달받아 운동방을 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = ChallengeRoomHttp.CREATE_ROOM_SUCCESS_CODE,
                description = ChallengeRoomHttp.CREATE_ROOM_SUCCESS_MESSAGE,
                content = [
                    Content(schema = Schema(implementation = CreateRoomResponse::class))
                ]
            )
        ]
    )
    fun createRoom(
        @RequestBody @Valid request: CreateRoomRequest
    ): ResponseData<CreateRoomResponse>

    @GetMapping
    @Operation(summary = "Get Challenge Rooms", description = "운동방 전체 목록을 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = ChallengeRoomHttp.GET_ROOMS_SUCCESS_CODE,
                description = ChallengeRoomHttp.GET_ROOMS_SUCCESS_MESSAGE,
                content = [
                    Content(schema = Schema(implementation = GetRoomsResponse::class))
                ]
            )
        ]
    )
    fun getRooms(
        @RequestParam(name = "page", defaultValue = ChallengeRoomConstants.DEFAULT_PAGE) page: Int,
        @RequestParam(name = "size", defaultValue = ChallengeRoomConstants.DEFAULT_SIZE) size: Int
    ): ResponseData<GetRoomsResponse>

    @GetMapping("/{roomId}")
    @Operation(summary = "Get Challenge Room", description = "특정 운동방 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = ChallengeRoomHttp.GET_ROOM_SUCCESS_CODE,
                description = ChallengeRoomHttp.GET_ROOM_SUCCESS_MESSAGE,
                content = [
                    Content(schema = Schema(implementation = GetRoomResponse::class))
                ]
            )
        ]
    )
    fun getRoom(@PathVariable("roomId") @NotNull roomId: Long): ResponseData<GetRoomResponse>

    @DeleteMapping("/{roomId}")
    @Operation(summary = "Delete Challenge Room", description = "특정 운동방을 삭제합니다.")
    @ApiResponses(
        ApiResponse(
            responseCode = ChallengeRoomHttp.DELETE_ROOM_SUCCESS_CODE,
            description = ChallengeRoomHttp.DELETE_ROOM_SUCCESS_MESSAGE
        )
    )
    fun deleteRoom(@PathVariable("roomId") @NotNull roomId: Long): ResponseData<Void>
}
