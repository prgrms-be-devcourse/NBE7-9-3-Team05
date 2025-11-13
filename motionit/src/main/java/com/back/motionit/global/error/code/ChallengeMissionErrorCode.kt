package com.back.motionit.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChallengeMissionErrorCode implements ErrorCode {
	NOT_FOUND_USER(HttpStatus.NOT_FOUND, "R-700", "유저를 찾을 수 없습니다."),
	NOT_FOUND_VIDEO(HttpStatus.NOT_FOUND, "R-701", "챌린지 영상을 찾을 수 없습니다."),
	NOT_INITIALIZED_MISSION(HttpStatus.BAD_REQUEST, "R-702", "오늘의 미션이 초기화되지 않았습니다."),
	INVALID_ROOM_ACCESS(HttpStatus.FORBIDDEN, "R-703", "챌린지 방에 접근할 수 없습니다."),
	ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "R-704", "오늘의 미션을 이미 완료했습니다."),
	NO_VIDEO_UPLOADED(HttpStatus.BAD_REQUEST, "R-705", "완료할 오늘이 미션이 등록되지 않았습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
