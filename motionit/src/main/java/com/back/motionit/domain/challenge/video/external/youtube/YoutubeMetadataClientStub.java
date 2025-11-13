package com.back.motionit.domain.challenge.video.external.youtube;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata;

@Component
@Profile({"perf", "test"}) // perf 프로필에서만 활성화됨
public class YoutubeMetadataClientStub extends YoutubeMetadataClient {

	@Override
	public YoutubeVideoMetadata fetchMetadata(String youtubeUrl) {
		String videoId = extractVideoId(youtubeUrl); // 부모 클래스 메서드 그대로 사용

		return YoutubeVideoMetadata.builder()
			.videoId(videoId)
			.title("Performance Test Video - " + videoId)
			.thumbnailUrl("https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg")
			.durationSeconds(120)
			.build();
	}
}
