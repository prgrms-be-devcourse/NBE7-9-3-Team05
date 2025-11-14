package com.back.motionit.domain.challenge.video.entity;

import java.time.LocalDate;

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Table(
	name = "challenge_videos",
	uniqueConstraints = {
		@UniqueConstraint(
			columnNames = {"challenge_room_id", "youtube_video_id"},
			name = "uk_challenge_room_youtube_video"
		)
	}
)
public class ChallengeVideo extends BaseEntity {
	// 어떤 방의 영상인지
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "challenge_room_id", nullable = false)
	private ChallengeRoom challengeRoom;

	// 누가 업로드했는지
	@ManyToOne(fetch = FetchType.LAZY)
	private User user;

	// 유튜브 비디오 식별자
	@Column(name = "youtube_video_id", nullable = false)
	private String youtubeVideoId;

	// 영상 제목 (프론트 표시용)
	@Column(nullable = false)
	private String title;

	// 썸네일 URL
	@Column(nullable = false, name = "thumbnail_url")
	private String thumbnailUrl;

	// 재생 시간 (초 단위)
	@Column(nullable = false)
	private Integer duration;

	// 업로드된 날짜
	@Column(nullable = false, name = "upload_date")
	private LocalDate uploadDate;

	// 오늘의 미션 영상 여부
	@Column(nullable = false, name = "is_today_mission")
	private Boolean isTodayMission;

	public static ChallengeVideo of(ChallengeRoom room, User user, YoutubeVideoMetadata metadata,
		boolean isTodayMission) {
		return ChallengeVideo.builder()
			.challengeRoom(room)
			.user(user)
			.youtubeVideoId(metadata.getVideoId())
			.title(metadata.getTitle())
			.thumbnailUrl(metadata.getThumbnailUrl())
			.duration(metadata.getDurationSeconds())
			.uploadDate(LocalDate.now())
			.isTodayMission(isTodayMission)
			.build();
	}
}
