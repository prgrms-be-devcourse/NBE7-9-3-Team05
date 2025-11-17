package com.back.motionit.domain.challenge.video.dto

data class YoutubeVideoPayload(
    val userId: Long,
    val roomId: Long,
    val youtubeUrl: String,
)
