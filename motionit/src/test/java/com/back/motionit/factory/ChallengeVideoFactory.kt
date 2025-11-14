package com.back.motionit.factory

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import com.back.motionit.domain.user.entity.User
import java.time.LocalDate

object ChallengeVideoFactory : BaseFactory() {
	fun fakeChallengeVideo(user: User, room: ChallengeRoom): ChallengeVideo =
        ChallengeVideo.fake(
            room,
            user,
            generateYoutubeVideoId(),
            faker.lorem().sentence(3),
            faker.internet().url(),
            faker.number().numberBetween(30, 600),
            LocalDate.now(),
            true
        )

    private fun generateYoutubeVideoId(): String = faker.regexify("[A-Za-z0-9_-]{11}")
}
