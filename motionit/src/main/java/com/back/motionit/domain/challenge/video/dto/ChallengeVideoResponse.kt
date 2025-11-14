package com.back.motionit.domain.challenge.video.dto

import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import java.time.LocalDate

data class ChallengeVideoResponse(
    val id: Long,
    val youtubeVideoId: String,
    val title: String,
    val thumbnailUrl: String,
    val duration: Int,
    val uploadDate: LocalDate,
    val isTodayMission: Boolean,
    val uploaderId: Long,
    val roomId: Long
) {
    companion object {
        @JvmStatic
        fun from(video: ChallengeVideo): ChallengeVideoResponse {
            return ChallengeVideoResponse(
                id = video.id!!,
                youtubeVideoId = video.youtubeVideoId,
                title = video.title,
                thumbnailUrl = video.thumbnailUrl,
                duration = video.duration,
                uploadDate = video.uploadDate,
                isTodayMission = video.isTodayMission,
                uploaderId = video.user.id!!,
                roomId = video.challengeRoom.id!!
            )
        }
    }
}
