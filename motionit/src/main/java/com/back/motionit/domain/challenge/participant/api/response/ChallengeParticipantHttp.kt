package com.back.motionit.domain.challenge.participant.api.response

object ChallengeParticipantHttp {
    // 성공
    const val JOIN_SUCCESS_CODE: String = "201"
    const val JOIN_SUCCESS_MESSAGE: String = "운동방 참가 완료"

    const val LEAVE_SUCCESS_CODE: String = "200"
    const val LEAVE_SUCCESS_MESSAGE: String = "운동방 탈퇴 완료"

    const val GET_PARTICIPANT_LIST_SUCCESS_MESSAGE: String = "참여자 리스트 조회 성공"
    const val GET_PARTICIPANT_STATUS_SUCCESS_MESSAGE: String = "참여자 상태 조회 성공"
}
