package com.back.motionit.domain.challenge.video.dto;

import java.time.LocalDate;

import com.back.motionit.domain.challenge.video.entity.ChallengeVideo;

import lombok.Builder;

@Builder
public record ChallengeVideoResponse(
	Long id,
	String youtubeVideoId,
	String title,
	String thumbnailUrl,
	Integer duration,
	LocalDate uploadDate,
	boolean isTodayMission,
	Long uploaderId,
	Long roomId
) {
	public static ChallengeVideoResponse from(ChallengeVideo video) {
		return ChallengeVideoResponse.builder()
			.id(video.getId())
			.youtubeVideoId(video.getYoutubeVideoId())
			.title(video.getTitle())
			.thumbnailUrl(video.getThumbnailUrl())
			.duration(video.getDuration())
			.uploadDate(video.getUploadDate())
			.isTodayMission(video.isTodayMission())
			.uploaderId(video.getUser().getId())
			.roomId(video.getChallengeRoom().getId())
			.build();
	}
}
