package com.back.motionit.factory

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.video.entity.OpenStatus
import com.back.motionit.domain.user.entity.User
import java.time.LocalDateTime

object ChallengeRoomFactory : BaseFactory() {
    fun fakeChallengeRoom(user: User, capacity: Int = faker.number().numberBetween(2, 100)): ChallengeRoom {
        val now = LocalDateTime.now()
        val startOffsetDays = faker.number().numberBetween(0, 7) // 오늘~7일 내 시작
        val durationDays = faker.number().numberBetween(7, 30) // 1~4주 진행

        val start = now.plusDays(startOffsetDays.toLong())
        val end = start.plusDays(durationDays.toLong())

        return buildRoom(user, capacity, start, end)
    }

    private fun buildRoom(
        user: User,
        capacity: Int,
        start: LocalDateTime,
        end: LocalDateTime
    ): ChallengeRoom = ChallengeRoom(
        user,
        faker.lorem().sentence(3, 5),  // title
        faker.lorem().paragraph(),  // description
        capacity,
        faker.options().option(OpenStatus::class.java),
        start,
        end,
        faker.internet().url(),
        null
    )
}
