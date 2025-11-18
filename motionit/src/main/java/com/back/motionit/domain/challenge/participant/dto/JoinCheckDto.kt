package com.back.motionit.domain.challenge.participant.dto

data class JoinCheckDto(
    val alreadyJoined: Long,
    val currentCount: Long
)