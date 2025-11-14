package com.back.motionit.factory;

import java.time.LocalDate;

import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo;

public class ChallengeMissionStatusFactory extends BaseFactory {
	// 외부에서 new로 인스턴스 생성 방지
	private ChallengeMissionStatusFactory() {
	}

	/**
	 *  오늘 날짜 기준 미완료 미션 생성
	 */
	public static ChallengeMissionStatus fakeMission(ChallengeParticipant participant) {
		return fakeMission(participant, LocalDate.now(), false);
	}

	/**
	 *  오늘 날짜 기준 완료 미션 생성 (영상 연결 포함)
	 */
	public static ChallengeMissionStatus fakeCompletedMission(ChallengeParticipant participant) {
		return fakeMission(participant, LocalDate.now(), true);
	}

	/**
	 *  커스텀 날짜 및 완료 여부 지정 가능
	 */
	public static ChallengeMissionStatus fakeMission(
		ChallengeParticipant participant,
		LocalDate missionDate,
		boolean completed
	) {
		return ChallengeMissionStatus.builder()
			.participant(participant)
			.missionDate(missionDate)
			.completed(completed)
			.build();
	}

	/**
	 *  과거 날짜 미션 생성 ( 기록 조회 테스트용)
	 */
	public static ChallengeMissionStatus fakePastMission(ChallengeParticipant participant, ChallengeVideo video) {
		LocalDate pastDate = LocalDate.now().minusDays(faker.number().numberBetween(1, 7));
		return fakeMission(participant, pastDate, faker.bool().bool());
	}

	/**
	 *  랜덤 완료 여부로 생성 (랜덤 데이터 다량 삽입용)
	 */
	public static ChallengeMissionStatus fakeRandomMission(ChallengeParticipant participant, ChallengeVideo video) {
		return fakeMission(
			participant,
			LocalDate.now(),
			faker.bool().bool()
		);
	}
}
