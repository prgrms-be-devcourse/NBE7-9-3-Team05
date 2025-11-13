package com.back.motionit.global.error.code

import org.springframework.http.HttpStatus

enum class ChallengeVideoErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "R-600", "유저를 찾을 수 없습니다."),
    CANNOT_FIND_CHALLENGE_ROOM(HttpStatus.NOT_FOUND, "R-601", "챌린지 룸을 찾을 수 없습니다."),
    INVALID_VIDEO_FORMAT(HttpStatus.BAD_REQUEST, "R-602", "유효하지 않은 비디오 형식입니다."),
    DUPLICATE_VIDEO_IN_ROOM(HttpStatus.BAD_REQUEST, "R-603", "이미 해당 챌린지 룸에 업로드된 영상입니다."),
    VIDEO_NOT_FOUND_OR_FORBIDDEN(HttpStatus.NOT_FOUND, "R-604", "영상을 찾을 수 없거나 접근이 금지되었습니다."),
    USER_NOT_PARTICIPANT_IN_ROOM(HttpStatus.UNAUTHORIZED, "R-605", "운동방의 참가자가 아닙니다.")
}