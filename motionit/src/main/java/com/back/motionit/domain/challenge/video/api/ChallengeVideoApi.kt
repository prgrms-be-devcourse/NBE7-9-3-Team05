package com.back.motionit.domain.challenge.video.api;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.back.motionit.domain.challenge.video.api.response.ChallengeVideoHttp;
import com.back.motionit.domain.challenge.video.dto.ChallengeVideoResponse;
import com.back.motionit.domain.challenge.video.dto.ChallengeVideoUploadRequest;
import com.back.motionit.global.respoonsedata.ResponseData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "ApiV1ChallengeVideoController", description = "운동방 유튜브 영상 업로드 및 관리 API")
public interface ChallengeVideoApi {
	@PostMapping("/api/v1/challenge/rooms/{roomId}/videos")
	@Operation(summary = "유튜브 영상 업로드",
		description = "해당 운동방에 유튜브 영상을 업로드합니다.",
		responses = {
				@ApiResponse(
					responseCode = ChallengeVideoHttp.UPLOAD_SUCCESS_CODE,
					description = ChallengeVideoHttp.UPLOAD_SUCCESS_MESSAGE
			),
				@ApiResponse(responseCode = "400", description = "중복된 영상 업로드 / 존재하지 않는 유저 또는 방"),
				@ApiResponse(responseCode = "500", description = "유튜브 API 호출 실패")
		})
	ResponseData<ChallengeVideoResponse> uploadVideo(
		@PathVariable("roomId") Long roomId,
		@RequestBody @Valid ChallengeVideoUploadRequest request
	);

	@GetMapping("/api/v1/challenge/rooms/{roomId}/videos/today")
	@Operation(summary = "오늘의 미션 영상 조회",
		description = "해당 운동방에서 오늘 업로드된 미션 영상을 조회합니다.",
		responses = {
				@ApiResponse(
					responseCode = ChallengeVideoHttp.GET_TODAY_MISSION_SUCCESS_CODE,
					description = ChallengeVideoHttp.GET_TODAY_MISSION_SUCCESS_MESSAGE
			)
		})
	ResponseData<List<ChallengeVideoResponse>> getTodayMissionVideos(
		@PathVariable("roomId") Long roomId
	);

	@DeleteMapping("/api/v1/challenge/rooms/{roomId}/videos/{videoId}")
	@Operation(summary = "영상 삭제",
		description = "본인이 업로드한 영상을 삭제합니다.",
		responses = {
				@ApiResponse(
					responseCode = ChallengeVideoHttp.DELETE_SUCCESS_CODE,
					description = ChallengeVideoHttp.DELETE_SUCCESS_MESSAGE
			),
				@ApiResponse(responseCode = "400", description = "본인 외 영상 삭제 시도"),
				@ApiResponse(responseCode = "404", description = "존재하지 않는 영상")
		})
	ResponseData<Void> deleteVideoByUser(
		@PathVariable("roomId") Long roomId,
		@PathVariable("videoId") Long videoId
	);
}
