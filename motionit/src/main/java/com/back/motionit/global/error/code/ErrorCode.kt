package com.back.motionit.global.error.code;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
	String getMessage();

	HttpStatus getStatus();

	String getCode();
}
