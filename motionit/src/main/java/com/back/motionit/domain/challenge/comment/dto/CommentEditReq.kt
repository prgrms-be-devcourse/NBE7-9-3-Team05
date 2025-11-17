package com.back.motionit.domain.challenge.comment.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CommentEditReq(
    @field:NotBlank
    @field:Size(max = 1000)
    val content: String
)