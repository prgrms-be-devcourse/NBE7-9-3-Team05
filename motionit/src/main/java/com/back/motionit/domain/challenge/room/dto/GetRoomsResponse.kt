package com.back.motionit.domain.challenge.room.dto;

import java.util.List;

public record GetRoomsResponse(
	int total,
	List<GetRoomSummary> rooms
) {
}
