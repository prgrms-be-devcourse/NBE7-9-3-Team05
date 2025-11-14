package com.back.motionit.domain.challenge.room.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChallengeRoomSummaryRepositoryImpl implements ChallengeRoomSummaryRepository {

	private final EntityManager manager;

	@Override
	public Page<ChallengeRoom> fetchOpenRooms(Pageable pageable) {
		var data = manager.createQuery("""
					select r
					from ChallengeRoom r
					where r.openStatus = com.back.motionit.domain.challenge.video.entity.OpenStatus.OPEN
					order by r.createDate desc
				""", ChallengeRoom.class)
			.setFirstResult((int)pageable.getOffset())
			.setMaxResults(pageable.getPageSize())
			.getResultList();

		long total = countOpenRooms();
		return new PageImpl<>(data, pageable, total);
	}

	@Override
	public int countOpenRooms() {
		Long cnt = manager.createQuery("""
				select count(r)
				from ChallengeRoom r
				where r.openStatus = com.back.motionit.domain.challenge.video.entity.OpenStatus.OPEN
			""", Long.class).getSingleResult();
		return cnt.intValue();
	}
}
