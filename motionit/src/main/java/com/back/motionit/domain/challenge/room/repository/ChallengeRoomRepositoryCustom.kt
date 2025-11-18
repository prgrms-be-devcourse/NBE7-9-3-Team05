package com.back.motionit.domain.challenge.room.repository

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom

interface ChallengeRoomRepositoryCustom {
    fun findDetailById(id: Long): ChallengeRoom?
}
