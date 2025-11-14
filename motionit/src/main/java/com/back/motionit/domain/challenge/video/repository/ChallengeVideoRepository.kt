package com.back.motionit.domain.challenge.video.repository

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ChallengeVideoRepository : JpaRepository<ChallengeVideo, Long> {
    fun existsByChallengeRoomAndYoutubeVideoId(challengeRoom: ChallengeRoom, videoId: String): Boolean

    fun findByUserIdAndUploadDate(userId: Long, today: LocalDate): List<ChallengeVideo>

    fun findByIdAndUserId(videoId: Long, userId: Long): ChallengeVideo?

    fun findByChallengeRoomId(roomId: Long): List<ChallengeVideo>

    fun existsByChallengeRoomIdAndUploadDate(roomId: Long, today: LocalDate): Boolean
}
