package com.back.motionit.global.error.util

import com.back.motionit.global.error.code.ErrorCode
import com.back.motionit.global.respoonsedata.ResponseData
import org.springframework.http.ResponseEntity

object ErrorResponse {
    // TODO: JvmStatic 제거
    @JvmStatic
    fun build(errorCode: ErrorCode): ResponseEntity<ResponseData<Void>> = ResponseEntity
        .status(errorCode.status)
        .body(ResponseData.error(errorCode))
}