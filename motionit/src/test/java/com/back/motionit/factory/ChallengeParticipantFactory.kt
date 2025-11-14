package com.back.motionit.factory;

import java.util.List;

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.user.entity.User;

public class ChallengeParticipantFactory extends BaseFactory {

	private ChallengeParticipantFactory() {
	}

	/**
	 * 기본 NORMAL 참가자 생성
	 */
	public static ChallengeParticipant fakeParticipant(User user, ChallengeRoom room) {
		return fakeParticipant(user, room, ChallengeParticipantRole.NORMAL, false);
	}

	/**
	 * 역할과 탈퇴여부를 지정해 생성
	 */
	public static ChallengeParticipant fakeParticipant(
		User user,
		ChallengeRoom room,
		ChallengeParticipantRole role,
		boolean quited
	) {
		return ChallengeParticipant.create(
			user,
			room,
			role,
			quited,
			false,
			null
		);
	}

	/**
	 * HOST 참가자 생성
	 */
	public static ChallengeParticipant fakeHost(User user, ChallengeRoom room) {
		return fakeParticipant(user, room, ChallengeParticipantRole.HOST, false);
	}

	/**
	 * 여러 참가자 한번에 생성 (테스트 편의)
	 */
	public static List<ChallengeParticipant> fakeParticipants(User owner, ChallengeRoom room, int count) {
		return faker.collection(() ->
			fakeParticipant(
				UserFactory.fakeUser(),  // 랜덤 유저
				room,
				ChallengeParticipantRole.NORMAL,
				false
			)
		).len(count).generate();
	}
}
