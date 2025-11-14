package com.back.motionit.domain.challenge.room.controller

import com.back.motionit.domain.challenge.room.api.ChallengeRoomApi
import com.back.motionit.domain.challenge.room.api.response.ChallengeRoomHttp
import com.back.motionit.domain.challenge.room.dto.CreateRoomRequest
import com.back.motionit.domain.challenge.room.dto.CreateRoomResponse
import com.back.motionit.domain.challenge.room.dto.GetRoomResponse
import com.back.motionit.domain.challenge.room.dto.GetRoomsResponse
import com.back.motionit.domain.challenge.room.service.ChallengeRoomService
import com.back.motionit.global.constants.ChallengeRoomConstants
import com.back.motionit.global.request.RequestContext
import com.back.motionit.global.respoonsedata.ResponseData
import com.back.motionit.global.respoonsedata.ResponseData.Companion.success
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/challenge/rooms")
@Validated
class ChallengeRoomController(
    private val challengeRoomService: ChallengeRoomService,
    private val httpRequest: RequestContext
) : ChallengeRoomApi {

    override fun createRoom(@RequestBody request: @Valid CreateRoomRequest): ResponseData<CreateRoomResponse> {
        val user = httpRequest.actor
        val response = challengeRoomService.createRoom(request, user)
        return success(
            ChallengeRoomHttp.CREATE_ROOM_SUCCESS_CODE,
            ChallengeRoomHttp.CREATE_ROOM_SUCCESS_MESSAGE, response
        )
    }

    override fun getRooms(
        @RequestParam(name = "page", defaultValue = ChallengeRoomConstants.DEFAULT_PAGE) page: Int,
        @RequestParam(name = "size", defaultValue = ChallengeRoomConstants.DEFAULT_SIZE) size: Int
    ): ResponseData<GetRoomsResponse> {
        val user = httpRequest.actor
        val response = challengeRoomService.getRooms(user, page, size)
        return success(
            ChallengeRoomHttp.GET_ROOMS_SUCCESS_CODE,
            ChallengeRoomHttp.GET_ROOMS_SUCCESS_MESSAGE, response
        )
    }

    override fun getRoom(@PathVariable("roomId") @NotNull roomId: Long): ResponseData<GetRoomResponse> {
        val response = challengeRoomService.getRoom(roomId)
        return success(
            ChallengeRoomHttp.GET_ROOM_SUCCESS_CODE,
            ChallengeRoomHttp.GET_ROOM_SUCCESS_MESSAGE, response
        )
    }

    override fun deleteRoom(@PathVariable("roomId") @NotNull roomId: Long): ResponseData<Void> {
        val user = httpRequest.actor
        challengeRoomService.deleteRoom(roomId, user)
        return success(
            ChallengeRoomHttp.DELETE_ROOM_SUCCESS_CODE,
            ChallengeRoomHttp.DELETE_ROOM_SUCCESS_MESSAGE, null
        )
    }
}
