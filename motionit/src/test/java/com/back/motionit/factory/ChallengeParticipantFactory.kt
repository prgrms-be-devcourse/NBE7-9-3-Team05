package com.back.motionit.factory

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant.Companion.create
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.user.entity.User

object ChallengeParticipantFactory : BaseFactory() {
    /**
     * 역할과 탈퇴여부를 지정해 생성
     */
    /**
     * 기본 NORMAL 참가자 생성
     */
    fun fakeParticipant(
        user: User,
        room: ChallengeRoom,
        role: ChallengeParticipantRole = ChallengeParticipantRole.NORMAL,
        quited: Boolean = false
    ): ChallengeParticipant = create(
        user,
        room,
        role,
        quited,
        false,
        null
    )

    /**
     * HOST 참가자 생성
     */
	fun fakeHost(user: User, room: ChallengeRoom): ChallengeParticipant =
        fakeParticipant(user, room, ChallengeParticipantRole.HOST, false)
}
