package com.back.motionit.domain.challenge.video.external.youtube;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata;
import com.back.motionit.global.error.code.ChallengeVideoErrorCode;
import com.back.motionit.global.error.exception.BusinessException;

@Component
@Profile("!perf & !test")
public class YoutubeMetadataClient {

	// YouTube Data API v3 공식 엔드포인트
	private static final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/videos";
	@Value("${youtube.api.key}")
	private String apiKey;

	// videoId를 추출하고 메타데이터를 가져오는 메서드
	public YoutubeVideoMetadata fetchMetadata(String youtubeUrl) {
		// video ID 추출
		String videoId = extractVideoId(youtubeUrl);

		// YouTube Data API 호출 URL 구성
		String url = YOUTUBE_API_URL + "?id=" + videoId + "&part=snippet,contentDetails&key=" + apiKey;

		// TODO: timeout 설정 등 추가 구성 필요
		RestTemplate restTemplate = new RestTemplate();
		Map response = restTemplate.getForObject(url, Map.class);

		// 응답에서 필요한 데이터 추출
		List<Map> items = (List<Map>)response.get("items");
		if (items == null || items.isEmpty()) {
			throw new BusinessException(ChallengeVideoErrorCode.INVALID_VIDEO_FORMAT);
		}

		// 데이터 추출 및 YoutubeVideoMetadata dto에 매핑
		Map snippet = (Map)items.get(0).get("snippet");
		Map contentDetails = (Map)items.get(0).get("contentDetails");
		return new YoutubeVideoMetadata(
			videoId,
			(String)snippet.get("title"),
			(String)((Map<String, Map>)snippet.get("thumbnails")).get("high").get("url"),
			parseDuration((String)contentDetails.get("duration"))
		);

		// return YoutubeVideoMetadata.builder()
		// 	.videoId(videoId)
		// 	.title((String)snippet.get("title"))
		// 	.thumbnailUrl((String)((Map<String, Map>)snippet.get("thumbnails")).get("high").get("url"))
		// 	.durationSeconds(parseDuration((String)contentDetails.get("duration")))
		// 	.build();
	}

	// 유튜브 URL에서 비디오 ID를 추출하는 헬퍼 메서드, watch?v=VIDEO_ID 형식 가정
	public String extractVideoId(String youtubeUrl) {
		if (youtubeUrl.contains("v=")) {
			return youtubeUrl.split("v=")[1].split("&")[0];
		}
		throw new BusinessException(ChallengeVideoErrorCode.INVALID_VIDEO_FORMAT);
	}

	// ISO 8601 형식의 기간 문자열을 초 단위 정수로 변환하는 헬퍼 메서드
	private int parseDuration(String isoDuration) {
		Duration duration = Duration.parse(isoDuration);
		return (int)duration.getSeconds();
	}
}
