package com.back.motionit.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConfigErrorCode implements ErrorCode {
	FAILED_SIGN_CLOUD_FRONT_URL(HttpStatus.BAD_REQUEST, "S-001", "AWS CloudFront URL Sign 실패"),
	FAILED_LOAD_PRIVATE_KEY(HttpStatus.BAD_REQUEST, "S-002", "AWS private key 로드 실패");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
