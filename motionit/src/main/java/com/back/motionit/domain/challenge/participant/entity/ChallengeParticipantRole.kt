package com.back.motionit.domain.challenge.participant.entity

enum class ChallengeParticipantRole(
    private val value: String
) {
    HOST("host"),
    NORMAL("normal");

    fun getValue(): String = value
}
