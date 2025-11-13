package com.back.motionit.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

	WRONG_ACCESS(HttpStatus.FORBIDDEN, "M-101", "본인이 작성한 댓글이 아닙니다."),
	COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "M-102", "댓글을 찾을 수 없습니다."),
	ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "M-103", "운동방을 찾을 수 없습니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "M-104", "사용자를 찾을 수 없습니다."),
	INAPPROPRIATE_CONTENT_WARN(HttpStatus.UNPROCESSABLE_ENTITY, "M-105",
		"공격적으로 인식될 수 있습니다. 완화된 표현으로 다시 작성해 주세요."),
	INAPPROPRIATE_CONTENT_BLOCK(HttpStatus.FORBIDDEN, "M-106",
		"강한 욕설이나 비하 표현은 허용되지 않습니다.");
	private final HttpStatus status;
	private final String code;
	private final String message;
}
