package com.back.motionit.domain.challenge.room.dto

import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import java.time.LocalDate

data class ChallengeVideoDto(
    val id: Long?,
    val youtubeVideoId: String,

    val title: String,

    val thumbnailUrl: String,
    val duration: Int,
    val uploadDate: LocalDate,
    val isTodayMission: Boolean
) {
    constructor(video: ChallengeVideo) : this(
        video.id,
        video.youtubeVideoId,
        video.title,
        video.thumbnailUrl,
        video.duration,
        video.uploadDate,
        video.isTodayMission
    )
}
