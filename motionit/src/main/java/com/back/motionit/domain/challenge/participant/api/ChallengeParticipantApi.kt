package com.back.motionit.domain.challenge.participant.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.back.motionit.domain.challenge.participant.api.response.ChallengeParticipantHttp;
import com.back.motionit.domain.challenge.participant.dto.ChallengeParticipantResponse;
import com.back.motionit.global.respoonsedata.ResponseData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;

@Tag(name = "ApiV1ChallengeParticipantController", description = "운동방 참여 API")
public interface ChallengeParticipantApi {

	@PostMapping("/{roomId}/join")
	@Operation(
		summary = "운동방 참가",
		description = "운동방에 참가합니다. 이미 참가한 유저는 중복 참가할 수 없습니다. 정원이 초과된 방에는 참가할 수 없습니다.",
		responses = {
				@ApiResponse(
					responseCode = ChallengeParticipantHttp.JOIN_SUCCESS_CODE,
					description = ChallengeParticipantHttp.JOIN_SUCCESS_MESSAGE
				),
				@ApiResponse(responseCode = "400", description = "이미 참가한 유저 / 정원 초과"),
				@ApiResponse(responseCode = "404", description = "존재하지 않는 방 또는 유저")
		})
	ResponseData<Void> joinChallengeRoom(
		@PathVariable("roomId") @NotNull Long roomId
	);

	@PostMapping("/{roomId}/leave")
	@Operation(summary = "챌린지 탈퇴",
		description = "챌린지에서 탈퇴합니다. 참가하지 않은 유저는 탈퇴할 수 없습니다.",
		responses = {
				@ApiResponse(
					responseCode = ChallengeParticipantHttp.LEAVE_SUCCESS_CODE,
					description = ChallengeParticipantHttp.LEAVE_SUCCESS_MESSAGE
			),
				@ApiResponse(responseCode = "400", description = "참가하지 않은 유저"),
				@ApiResponse(responseCode = "404", description = "존재하지 않는 방 또는 유저")
		})
	ResponseData<Void> leaveChallengeRoom(
		@PathVariable("roomId") @NotNull Long roomId
	);

	@GetMapping("/{roomId}/status")
	@Operation(summary = "사용자의 운동방 가입여부 조회",
		description = "현 로그인상태인 사용자가 해당 운동방 가입상태인지 조회"
	)
	ResponseData<ChallengeParticipantResponse> getParticipationStatus(@PathVariable("roomId") Long roomId);
}
