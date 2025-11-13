package com.back.motionit.global.error.code

import org.springframework.http.HttpStatus

enum class AiErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    //추후기능에 사용
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Z-001", "AI 서비스를 일시적으로 사용할 수 없습니다."),
    AI_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Z-002", "AI 요청 처리에 실패했습니다.")
}