package com.back.motionit.domain.challenge.room.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.motionit.domain.challenge.room.api.ChallengeRoomApi;
import com.back.motionit.domain.challenge.room.api.response.ChallengeRoomHttp;
import com.back.motionit.domain.challenge.room.dto.CreateRoomRequest;
import com.back.motionit.domain.challenge.room.dto.CreateRoomResponse;
import com.back.motionit.domain.challenge.room.dto.GetRoomResponse;
import com.back.motionit.domain.challenge.room.dto.GetRoomsResponse;
import com.back.motionit.domain.challenge.room.service.ChallengeRoomService;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.global.constants.ChallengeRoomConstants;
import com.back.motionit.global.request.RequestContext;
import com.back.motionit.global.respoonsedata.ResponseData;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/challenge/rooms")
@Validated
public class ChallengeRoomController implements ChallengeRoomApi {

	private final ChallengeRoomService challengeRoomService;

	private final RequestContext httpRequest;

	@Override
	public ResponseData<CreateRoomResponse> createRoom(@RequestBody @Valid CreateRoomRequest request) {
		User user = httpRequest.getActor();
		CreateRoomResponse response = challengeRoomService.createRoom(request, user);
		return ResponseData.success(ChallengeRoomHttp.CREATE_ROOM_SUCCESS_CODE,
			ChallengeRoomHttp.CREATE_ROOM_SUCCESS_MESSAGE, response);
	}

	@Override
	public ResponseData<GetRoomsResponse> getRooms(
		@RequestParam(name = "page", defaultValue = ChallengeRoomConstants.DEFAULT_PAGE) int page,
		@RequestParam(name = "size", defaultValue = ChallengeRoomConstants.DEFAULT_SIZE) int size
	) {
		User user = httpRequest.getActor();
		GetRoomsResponse response = challengeRoomService.getRooms(user, page, size);
		return ResponseData.success(ChallengeRoomHttp.GET_ROOMS_SUCCESS_CODE,
			ChallengeRoomHttp.GET_ROOMS_SUCCESS_MESSAGE, response);
	}

	@Override
	public ResponseData<GetRoomResponse> getRoom(@PathVariable("roomId") @NotNull Long roomId) {
		GetRoomResponse response = challengeRoomService.getRoom(roomId);
		return ResponseData.success(ChallengeRoomHttp.GET_ROOM_SUCCESS_CODE,
			ChallengeRoomHttp.GET_ROOM_SUCCESS_MESSAGE, response);
	}

	@Override
	public ResponseData<Void> deleteRoom(@PathVariable("roomId") @NotNull Long roomId) {
		User user = httpRequest.getActor();
		challengeRoomService.deleteRoom(roomId, user);
		return ResponseData.success(ChallengeRoomHttp.DELETE_ROOM_SUCCESS_CODE,
			ChallengeRoomHttp.DELETE_ROOM_SUCCESS_MESSAGE, null);
	}
}
