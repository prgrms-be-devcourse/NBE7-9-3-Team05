package com.back.motionit.domain.challenge.video.external.youtube.dto

data class YoutubeVideoMetadata(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
)