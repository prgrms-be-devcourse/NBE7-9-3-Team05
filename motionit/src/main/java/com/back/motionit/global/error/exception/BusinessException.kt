package com.back.motionit.global.error.exception

import com.back.motionit.global.error.code.ErrorCode


//TODO: 더욱 코틀린스럽게 수정 가능, 지금은 자바와 구조가 비슷함
class BusinessException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message) {
    constructor(errorCode: ErrorCode, detailMessage: String) :
            this(errorCode) {
        initCause(RuntimeException("${errorCode.message} - $detailMessage"))
    }
}