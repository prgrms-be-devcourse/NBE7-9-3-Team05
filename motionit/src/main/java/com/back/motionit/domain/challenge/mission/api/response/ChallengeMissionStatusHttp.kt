package com.back.motionit.domain.challenge.mission.api.response

object ChallengeMissionStatusHttp {
    // 성공
    const val MISSION_COMPLETE_SUCCESS_CODE: String = "200"
    const val MISSION_COMPLETE_SUCCESS_MESSAGE: String = "미션 완료 처리 성공"

    const val GET_TODAY_SUCCESS_CODE: String = "200"
    const val GET_TODAY_SUCCESS_MESSAGE: String = "오늘의 방 미션 현황 조회 성공"
    const val GET_TODAY_NO_MISSION_MESSAGE: String = "오늘 등록된 미션 데이터가 없습니다."

    const val GET_TODAY_PARTICIPANT_SUCCESS_CODE: String = "200"
    const val GET_TODAY_PARTICIPANT_SUCCESS_MESSAGE: String = "오늘의 참여자 미션 상태 조회 성공"

    const val GET_MISSION_HISTORY_SUCCESS_CODE: String = "200"
    const val GET_MISSION_HISTORY_SUCCESS_MESSAGE: String = "참여자 미션 이력 조회 성공"
}
