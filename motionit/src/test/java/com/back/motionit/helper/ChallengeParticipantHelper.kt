package com.back.motionit.helper

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.user.entity.User
import com.back.motionit.factory.ChallengeParticipantFactory.fakeHost
import com.back.motionit.factory.ChallengeParticipantFactory.fakeParticipant
import org.springframework.stereotype.Component

@Component
class ChallengeParticipantHelper(
    private val participantRepository: ChallengeParticipantRepository
) {
    fun createHostParticipant(user: User, room: ChallengeRoom): ChallengeParticipant =
        participantRepository.save(fakeHost(user, room))

    fun createNormalParticipant(user: User, room: ChallengeRoom): ChallengeParticipant =
        participantRepository.save(fakeParticipant(user, room))
}
