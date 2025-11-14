package com.back.motionit.factory;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.video.entity.OpenStatus;
import com.back.motionit.domain.user.entity.User;

public final class ChallengeRoomFactory extends BaseFactory {
	// public 생성자 막기, 불필요 객체 생성 방지
	// 예: new ChallengeRoomFactory().fakeChallengeRoom(user);
	private ChallengeRoomFactory() {
	}

	public static ChallengeRoom fakeChallengeRoom(User user) {
		return fakeChallengeRoom(user, faker.number().numberBetween(2, 100));
	}

	public static ChallengeRoom fakeChallengeRoom(User user, int capacity) {
		LocalDateTime now = LocalDateTime.now();
		int startOffsetDays = faker.number().numberBetween(0, 7);   // 오늘~7일 내 시작
		int durationDays = faker.number().numberBetween(7, 30);     // 1~4주 진행

		LocalDateTime start = now.plusDays(startOffsetDays);
		LocalDateTime end = start.plusDays(durationDays);

		return buildRoom(user, capacity, start, end);
	}

	private static ChallengeRoom buildRoom(User user, int capacity, LocalDateTime start, LocalDateTime end) {
		return new ChallengeRoom(
			user,
			faker.lorem().sentence(3, 5),          // title
			faker.lorem().paragraph(),             // description
			capacity,
			faker.options().option(OpenStatus.class),
			start,
			end,
			faker.internet().url(),
			null
		);
	}
}
