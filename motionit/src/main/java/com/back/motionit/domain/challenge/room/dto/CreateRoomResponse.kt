package com.back.motionit.domain.challenge.room.dto

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import com.back.motionit.domain.challenge.video.entity.OpenStatus
import java.time.LocalDateTime

data class CreateRoomResponse(
    val id: Long?,
    val title: String,
    val description: String,
    val capacity: Int,
    val openStatus: OpenStatus,
    val challengeStartDate: LocalDateTime,
    val challengeEndDate: LocalDateTime,
    val roomImage: String,
    val challengeVideoList: List<ChallengeVideo>,
    val uploadUrl: String
) {
    constructor(room: ChallengeRoom, uploadUrl: String) : this(
        room.id,
        room.title,
        room.description,
        room.capacity,
        room.openStatus,
        room.challengeStartDate,
        room.challengeEndDate,
        room.roomImage,
        room.getChallengeVideoList(),
        uploadUrl
    )
}
