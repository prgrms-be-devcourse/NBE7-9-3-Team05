package com.back.motionit.domain.challenge.video.controller

import com.back.motionit.domain.challenge.video.api.ChallengeVideoApi
import com.back.motionit.domain.challenge.video.api.response.ChallengeVideoHttp
import com.back.motionit.domain.challenge.video.dto.ChallengeVideoResponse
import com.back.motionit.domain.challenge.video.dto.ChallengeVideoUploadRequest
import com.back.motionit.domain.challenge.video.service.ChallengeVideoService
import com.back.motionit.global.request.RequestContext
import com.back.motionit.global.respoonsedata.ResponseData
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/challenge")
class ChallengeVideoController(
    private val challengeVideoService: ChallengeVideoService,
    private val requestContext: RequestContext,
) : ChallengeVideoApi {

    @PostMapping("/rooms/{roomId}/videos")
    override fun uploadVideo(
        @PathVariable("roomId") roomId: Long,
        @RequestBody request: @Valid ChallengeVideoUploadRequest
    ): ResponseData<ChallengeVideoResponse> {
        val actor = requestContext.actor

        challengeVideoService.requestUploadChallengeVideo(
            actor.id!!, roomId, request.youtubeUrl
        )
        return ResponseData.success(
            ChallengeVideoHttp.UPLOAD_SUCCESS_MESSAGE,
            null,
        )
    }

    @GetMapping("/rooms/{roomId}/videos/today")
    override fun getTodayMissionVideos(
        @PathVariable("roomId") roomId: Long
    ): ResponseData<List<ChallengeVideoResponse>> {

        val actor = requestContext.actor

        val videos = challengeVideoService
            .getTodayMissionVideos(actor.id!!, roomId)
            .map { ChallengeVideoResponse.from(it) }

        return ResponseData.success(
            ChallengeVideoHttp.GET_TODAY_MISSION_SUCCESS_MESSAGE,
            videos
        )
    }

    @DeleteMapping("/rooms/{roomId}/videos/{videoId}")
    override fun deleteVideoByUser(
        @PathVariable("roomId") roomId: Long,
        @PathVariable("videoId") videoId: Long
    ): ResponseData<Void> {

        val actor = requestContext.actor

        challengeVideoService.deleteVideoByUser(actor.id!!, roomId, videoId)

        return ResponseData.success(
            ChallengeVideoHttp.DELETE_SUCCESS_MESSAGE,
            null
        )
    }
}

// TODO: actor.id!! 없애기