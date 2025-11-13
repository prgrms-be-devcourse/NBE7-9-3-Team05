package com.back.motionit.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChallengeRoomErrorCode implements ErrorCode {
	NOT_FOUND_USER(HttpStatus.NOT_FOUND, "R-001", "유저를 찾을 수 없습니다."),
	NOT_FOUND_ROOM(HttpStatus.NOT_FOUND, "R-002", "운동방을 찾을 수 없습니다."),
	INVALID_AUTH_USER(HttpStatus.BAD_REQUEST, "R-003", "권한이 없는 사용자입니다."),
	FAILED_DELETE_ROOM(HttpStatus.BAD_REQUEST, "R-004", "방 삭제에 실패했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
