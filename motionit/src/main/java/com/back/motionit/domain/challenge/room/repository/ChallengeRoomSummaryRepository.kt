package com.back.motionit.domain.challenge.room.repository

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ChallengeRoomSummaryRepository {
    fun fetchOpenRooms(pageable: Pageable): Page<ChallengeRoom>

    fun countOpenRooms(): Int
}
