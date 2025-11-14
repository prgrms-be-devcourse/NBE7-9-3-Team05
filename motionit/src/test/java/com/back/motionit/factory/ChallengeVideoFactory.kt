package com.back.motionit.factory;

import java.time.LocalDate;

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo;
import com.back.motionit.domain.user.entity.User;

public final class ChallengeVideoFactory extends BaseFactory {

	private ChallengeVideoFactory() {
		// 인스턴스화 방지
	}

	public static ChallengeVideo fakeChallengeVideo(User user, ChallengeRoom room) {
		return ChallengeVideo.fake(
			room,
			user,
			generateYoutubeVideoId(),
			faker.lorem().sentence(3),
			faker.internet().url(),
			faker.number().numberBetween(30, 600),
			LocalDate.now(),
			true
		);
	}

	// 오늘 이전에 업로드된 영상 생성
	public static ChallengeVideo fakeOldVideo(User user, ChallengeRoom room) {
		return ChallengeVideo.fake(
			room,
			user,
			generateYoutubeVideoId(),
			faker.lorem().sentence(3),
			faker.internet().url(),
			faker.number().numberBetween(30, 600),
			LocalDate.now().minusDays(1),
			false
		);
	}

	private static String generateYoutubeVideoId() {
		return faker.regexify("[A-Za-z0-9_-]{11}");
	}
}
