package com.back.motionit.domain.challenge.mission.api.response;

public class ChallengeMissionStatusHttp {
	// 성공
	public static final String MISSION_COMPLETE_SUCCESS_CODE = "200";
	public static final String MISSION_COMPLETE_SUCCESS_MESSAGE = "미션 완료 처리 성공";

	public static final String GET_TODAY_SUCCESS_CODE = "200";
	public static final String GET_TODAY_SUCCESS_MESSAGE = "오늘의 방 미션 현황 조회 성공";
	public static final String GET_TODAY_NO_MISSION_MESSAGE = "오늘 등록된 미션 데이터가 없습니다.";

	public static final String GET_TODAY_PARTICIPANT_SUCCESS_CODE = "200";
	public static final String GET_TODAY_PARTICIPANT_SUCCESS_MESSAGE = "오늘의 참여자 미션 상태 조회 성공";

	public static final String GET_MISSION_HISTORY_SUCCESS_CODE = "200";
	public static final String GET_MISSION_HISTORY_SUCCESS_MESSAGE = "참여자 미션 이력 조회 성공";
}
