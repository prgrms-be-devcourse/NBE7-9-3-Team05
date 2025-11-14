package com.back.motionit.domain.challenge.video.dto

import jakarta.validation.constraints.NotBlank

data class ChallengeVideoUploadRequest(
    @JvmField
	@field:NotBlank
    val youtubeUrl: String
)
