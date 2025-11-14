package com.back.motionit.factory

import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.entity.User.Companion.builder
import java.util.concurrent.atomic.AtomicLong

object UserFactory : BaseFactory() {
    private val kakaoIdSequence = AtomicLong(1000000000L)

	fun fakeUser(): User = builder()
        .kakaoId(kakaoIdSequence.incrementAndGet())
        .email(faker.internet().emailAddress())
        .nickname(faker.name().firstName())
        .password(faker.name().firstName())
        .password(faker.internet().password(8, 16))
        .loginType(faker.options().option(LoginType::class.java))
        .userProfile(faker.internet().url())
        .build()
}
