package com.back.motionit.domain.challenge.mission.dto

import jakarta.validation.constraints.NotNull

data class ChallengeMissionCompleteRequest(
    @field:NotNull
    val videoId: Long
)
