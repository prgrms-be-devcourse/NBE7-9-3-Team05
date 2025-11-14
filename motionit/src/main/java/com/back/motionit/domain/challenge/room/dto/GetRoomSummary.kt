package com.back.motionit.domain.challenge.room.dto

import com.back.motionit.global.enums.ChallengeStatus

data class GetRoomSummary(
    val id: Long?,
    val title: String,
    val description: String,
    val capacity: Int,
    val dDay: Int,
    val roomImage: String,
    val status: ChallengeStatus,
    val current: Int
)
