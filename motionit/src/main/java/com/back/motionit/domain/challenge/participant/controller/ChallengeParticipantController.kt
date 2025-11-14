package com.back.motionit.domain.challenge.participant.controller

import com.back.motionit.domain.challenge.participant.api.ChallengeParticipantApi
import com.back.motionit.domain.challenge.participant.api.response.ChallengeParticipantHttp
import com.back.motionit.domain.challenge.participant.dto.ChallengeParticipantResponse
import com.back.motionit.domain.challenge.participant.service.ChallengeParticipantService
import com.back.motionit.domain.challenge.validator.ChallengeAuthValidator
import com.back.motionit.global.request.RequestContext
import com.back.motionit.global.respoonsedata.ResponseData
import com.back.motionit.global.respoonsedata.ResponseData.Companion.success
import jakarta.validation.constraints.NotNull
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/challenge/participants")
class ChallengeParticipantController(
    private val challengeParticipantService: ChallengeParticipantService,
    private val requestContext: RequestContext,
    private val challengeAuthValidator: ChallengeAuthValidator,
) : ChallengeParticipantApi {

    @PostMapping("/{roomId}/join")
    override fun joinChallengeRoom(
        @PathVariable("roomId") roomId: @NotNull Long
    ): ResponseData<Void> {
        val actor = requestContext.actor

        challengeParticipantService.joinChallengeRoom(actor.id!!, roomId)
        return success(ChallengeParticipantHttp.JOIN_SUCCESS_MESSAGE, null)
    }

    @PostMapping("/{roomId}/leave")
    override fun leaveChallengeRoom(
        @PathVariable("roomId") roomId: @NotNull Long
    ): ResponseData<Void> {
        val actor = requestContext.actor
        challengeAuthValidator.validateActiveParticipant(actor.id!!, roomId)

        challengeParticipantService.leaveChallenge(actor.id!!, roomId)
        return success(ChallengeParticipantHttp.LEAVE_SUCCESS_MESSAGE, null)
    }

    @GetMapping("/{roomId}/status")
    override fun getParticipationStatus(
        @PathVariable("roomId") roomId: Long
    ): ResponseData<ChallengeParticipantResponse> {
        val actor = requestContext.actor
        val joined = challengeParticipantService.isActiveParticipant(actor.id!!, roomId)

        return success(
            ChallengeParticipantHttp.GET_PARTICIPANT_STATUS_SUCCESS_MESSAGE,
            ChallengeParticipantResponse(actor.id!!, roomId, joined)
        )
    }
}

// TODO: actor.id!! null 체크 제거하기