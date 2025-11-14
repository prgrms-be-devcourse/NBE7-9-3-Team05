package com.back.motionit.domain.challenge.room.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;

public interface ChallengeRoomSummaryRepository {
	Page<ChallengeRoom> fetchOpenRooms(Pageable pageable);

	int countOpenRooms();
}
