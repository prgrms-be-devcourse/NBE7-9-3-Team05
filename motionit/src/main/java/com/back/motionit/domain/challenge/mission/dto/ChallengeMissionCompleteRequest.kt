package com.back.motionit.domain.challenge.mission.dto;

import jakarta.validation.constraints.NotNull;

public record ChallengeMissionCompleteRequest(
	@NotNull Long videoId
) {
}
