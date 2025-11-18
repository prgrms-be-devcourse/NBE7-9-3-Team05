package com.back.motionit.domain.challenge.video.repository

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface ChallengeVideoRepository : JpaRepository<ChallengeVideo, Long> {
    fun existsByChallengeRoomAndYoutubeVideoId(challengeRoom: ChallengeRoom, videoId: String): Boolean

    fun findByUserIdAndUploadDate(userId: Long, today: LocalDate): List<ChallengeVideo>

    fun findByIdAndUserId(videoId: Long, userId: Long): ChallengeVideo?

    fun findByChallengeRoomId(roomId: Long): List<ChallengeVideo>

    fun existsByChallengeRoomIdAndUploadDate(roomId: Long, today: LocalDate): Boolean

    @Query("""
        select v
        from ChallengeVideo v
        join fetch v.user
        join fetch v.challengeRoom
        where v.challengeRoom.id = :roomId
            and v.isTodayMission = true
            and v.uploadDate = :today
    """)
    fun findTodayVideos(roomId: Long, today: LocalDate): List<ChallengeVideo>
}
