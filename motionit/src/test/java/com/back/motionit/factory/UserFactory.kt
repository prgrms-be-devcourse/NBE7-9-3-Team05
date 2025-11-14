package com.back.motionit.factory;

import java.util.concurrent.atomic.AtomicLong;

import com.back.motionit.domain.user.entity.LoginType;
import com.back.motionit.domain.user.entity.User;

public final class UserFactory extends BaseFactory {
	private static final AtomicLong kakaoIdSequence = new AtomicLong(1000000000L);

	public static User fakeUser() {
		return User.builder()
			.kakaoId(kakaoIdSequence.incrementAndGet())
			.email(faker.internet().emailAddress())
			.nickname(faker.name().firstName())
			.password(faker.name().firstName())
			.password(faker.internet().password(8, 16))
			.loginType(faker.options().option(LoginType.class))
			.userProfile(faker.internet().url())
			.build();
	}
}
