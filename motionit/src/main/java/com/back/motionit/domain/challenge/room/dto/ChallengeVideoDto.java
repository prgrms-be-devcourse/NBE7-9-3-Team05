package com.back.motionit.domain.challenge.room.dto;

import java.time.LocalDate;

import com.back.motionit.domain.challenge.video.entity.ChallengeVideo;

public record ChallengeVideoDto(
	Long id,
	String youtubeVideoId,

	String title,

	String thumbnailUrl,
	Integer duration,
	LocalDate uploadDate,
	Boolean isTodayMission
) {
	public ChallengeVideoDto(ChallengeVideo video) {
		this(
			video.getId(),
			video.getYoutubeVideoId(),
			video.getTitle(),
			video.getThumbnailUrl(),
			video.getDuration(),
			video.getUploadDate(),
			video.isTodayMission()
		);
	}
}
