package com.back.motionit.domain.challenge.video.external.youtube.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class YoutubeVideoMetadata {
	private String videoId;
	private String title;
	private String thumbnailUrl;
	private Integer durationSeconds;
}
