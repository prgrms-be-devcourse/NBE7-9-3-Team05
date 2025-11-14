package com.back.motionit.domain.challenge.video.entity

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata
import com.back.motionit.domain.user.entity.User
import com.back.motionit.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "challenge_videos",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["challenge_room_id", "youtube_video_id"],
            name = "uk_challenge_room_youtube_video"
    )]
)
class ChallengeVideo(
    // 어떤 방의 영상인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_room_id", nullable = false)
    var challengeRoom: ChallengeRoom,

    // 누가 업로드했는지
    @ManyToOne(fetch = FetchType.LAZY)
    var user: User,

    // 유튜브 비디오 식별자
    @Column(name = "youtube_video_id", nullable = false)
    var youtubeVideoId: String,

    // 영상 제목 (프론트 표시용)
    @Column(nullable = false)
    var title: String,

    // 썸네일 URL
    @Column(nullable = false, name = "thumbnail_url")
    var thumbnailUrl: String,

    // 재생 시간 (초 단위)
    @Column(nullable = false)
    var duration: Int,

    // 업로드된 날짜
    @Column(nullable = false, name = "upload_date")
    var uploadDate: LocalDate,

    // 오늘의 미션 영상 여부
    @Column(nullable = false, name = "is_today_mission")
    var isTodayMission: Boolean,
) : BaseEntity() {

    companion object {
        @JvmStatic
        fun of(
            room: ChallengeRoom,
            user: User,
            metadata: YoutubeVideoMetadata,
            isTodayMission: Boolean
        ): ChallengeVideo {
            return ChallengeVideo(
                challengeRoom = room,
                user = user,
                youtubeVideoId = metadata.videoId,
                title = metadata.title,
                thumbnailUrl = metadata.thumbnailUrl,
                duration = metadata.durationSeconds,
                uploadDate = LocalDate.now(),
                isTodayMission = isTodayMission
            )
        }
        @JvmStatic
        fun fake(
            room: ChallengeRoom,
            user: User,
            youtubeVideoId: String,
            title: String,
            thumbnailUrl: String,
            duration: Int,
            uploadDate: LocalDate,
            isTodayMission: Boolean
        ): ChallengeVideo {
            return ChallengeVideo(
                challengeRoom = room,
                user = user,
                youtubeVideoId = youtubeVideoId,
                title = title,
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                uploadDate = uploadDate,
                isTodayMission = isTodayMission
            )
        }
    }
}
