package com.back.motionit.domain.challenge.participant.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.motionit.domain.challenge.participant.api.ChallengeParticipantApi;
import com.back.motionit.domain.challenge.participant.api.response.ChallengeParticipantHttp;
import com.back.motionit.domain.challenge.participant.dto.ChallengeParticipantResponse;
import com.back.motionit.domain.challenge.participant.service.ChallengeParticipantService;
import com.back.motionit.domain.challenge.validator.ChallengeAuthValidator;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.global.request.RequestContext;
import com.back.motionit.global.respoonsedata.ResponseData;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/challenge/participants")
@RequiredArgsConstructor
public class ChallengeParticipantController implements ChallengeParticipantApi {

	private final ChallengeParticipantService challengeParticipantService;
	private final RequestContext requestContext;
	private final ChallengeAuthValidator challengeAuthValidator;

	@PostMapping("/{roomId}/join")
	public ResponseData<Void> joinChallengeRoom(@PathVariable("roomId") @NotNull Long roomId) {
		User actor = requestContext.getActor();

		challengeParticipantService.joinChallengeRoom(actor.getId(), roomId);
		return ResponseData.success(ChallengeParticipantHttp.JOIN_SUCCESS_MESSAGE, null);
	}

	@PostMapping("/{roomId}/leave")
	public ResponseData<Void> leaveChallengeRoom(@PathVariable("roomId") @NotNull Long roomId) {
		User actor = requestContext.getActor();
		challengeAuthValidator.validateActiveParticipant(actor.getId(), roomId);

		challengeParticipantService.leaveChallenge(actor.getId(), roomId);
		return ResponseData.success(ChallengeParticipantHttp.LEAVE_SUCCESS_MESSAGE, null);
	}

	@GetMapping("/{roomId}/status")
	public ResponseData<ChallengeParticipantResponse> getParticipationStatus(@PathVariable("roomId") Long roomId) {
		User actor = requestContext.getActor();
		boolean joined = challengeParticipantService.isActiveParticipant(actor.getId(), roomId);

		return ResponseData.success(ChallengeParticipantHttp.GET_PARTICIPANT_STATUS_SUCCESS_MESSAGE,
			new ChallengeParticipantResponse(actor.getId(), roomId, joined));
	}

}
