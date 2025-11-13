package com.back.motionit.global.error.util;

import org.springframework.http.ResponseEntity;

import com.back.motionit.global.error.code.ErrorCode;
import com.back.motionit.global.respoonsedata.ResponseData;

public class ErrorResponse {

	private ErrorResponse() {
	}

	public static ResponseEntity<ResponseData<Void>> build(ErrorCode errorCode) {
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ResponseData.error(errorCode));
	}
}
