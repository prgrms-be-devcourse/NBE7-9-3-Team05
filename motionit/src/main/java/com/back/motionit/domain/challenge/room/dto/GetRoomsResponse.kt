package com.back.motionit.domain.challenge.room.dto

data class GetRoomsResponse(
	val total: Int,
	val rooms: List<GetRoomSummary>
)
