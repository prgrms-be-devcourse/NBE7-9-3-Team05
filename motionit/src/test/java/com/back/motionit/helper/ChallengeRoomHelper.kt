package com.back.motionit.helper

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.user.entity.User
import com.back.motionit.factory.ChallengeRoomFactory.fakeChallengeRoom
import org.springframework.stereotype.Component

@Component
class ChallengeRoomHelper internal constructor(
    private val challengeRoomRepository: ChallengeRoomRepository,
) {
    fun createChallengeRoom(user: User): ChallengeRoom =
        challengeRoomRepository.save(fakeChallengeRoom(user))
}
