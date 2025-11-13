package com.back.motionit.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentLikeErrorCode implements ErrorCode {
	COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "L-301", "댓글을 찾을 수 없습니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "L-302", "유저를 찾을 수 없습니다."),
	LIKE_TOGGLE_FAILED(HttpStatus.CONFLICT, "L-303", "좋아요 처리 중 동시성 충돌이 발생했습니다. 다시 시도해 주세요."),
	AUTHENTICATION_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "L-304", "인증된 사용자가 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
