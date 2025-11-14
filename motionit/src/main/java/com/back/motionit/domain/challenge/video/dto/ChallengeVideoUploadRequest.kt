package com.back.motionit.domain.challenge.video.dto;

import jakarta.validation.constraints.NotBlank;

public record ChallengeVideoUploadRequest(
	@NotBlank String youtubeUrl
) {
}
