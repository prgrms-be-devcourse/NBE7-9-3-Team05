package com.back.motionit.domain.challenge.participant.repository

import com.back.motionit.domain.challenge.participant.dto.JoinCheckDto

interface ChallengeParticipantCustom {

    fun checkJoinStatus(userId: Long, roomId: Long): JoinCheckDto
}