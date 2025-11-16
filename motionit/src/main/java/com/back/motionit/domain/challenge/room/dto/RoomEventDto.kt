package com.back.motionit.domain.challenge.room.dto

import com.back.motionit.global.enums.EventEnums

data class RoomEventDto(
    val event: String
) {
    constructor(event: EventEnums) : this(event.toString())
}
