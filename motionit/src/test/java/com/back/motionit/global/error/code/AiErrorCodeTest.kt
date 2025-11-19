package com.back.motionit.global.error.code

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

@DisplayName("AiErrorCode 테스트")
class AiErrorCodeTest {

    @Test
    @DisplayName("AI_SERVICE_UNAVAILABLE - 상태 코드 검증")
    fun aiServiceUnavailable_status() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, AiErrorCode.AI_SERVICE_UNAVAILABLE.status)
    }

    @Test
    @DisplayName("AI_SERVICE_UNAVAILABLE - 에러 코드 검증")
    fun aiServiceUnavailable_code() {
        assertEquals("Z-001", AiErrorCode.AI_SERVICE_UNAVAILABLE.code)
    }

    @Test
    @DisplayName("AI_SERVICE_UNAVAILABLE - 메시지 검증")
    fun aiServiceUnavailable_message() {
        assertEquals("AI 서비스를 일시적으로 사용할 수 없습니다.", AiErrorCode.AI_SERVICE_UNAVAILABLE.message)
    }

    @Test
    @DisplayName("AI_REQUEST_FAILED - 상태 코드 검증")
    fun aiRequestFailed_status() {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, AiErrorCode.AI_REQUEST_FAILED.status)
    }

    @Test
    @DisplayName("AI_REQUEST_FAILED - 에러 코드 검증")
    fun aiRequestFailed_code() {
        assertEquals("Z-002", AiErrorCode.AI_REQUEST_FAILED.code)
    }

    @Test
    @DisplayName("AI_REQUEST_FAILED - 메시지 검증")
    fun aiRequestFailed_message() {
        assertEquals("AI 요청 처리에 실패했습니다.", AiErrorCode.AI_REQUEST_FAILED.message)
    }

    @Test
    @DisplayName("모든 AiErrorCode enum 값 검증")
    fun allAiErrorCodes() {
        val values = AiErrorCode.values()
        assertEquals(2, values.size)
        assertEquals(AiErrorCode.AI_SERVICE_UNAVAILABLE, values[0])
        assertEquals(AiErrorCode.AI_REQUEST_FAILED, values[1])
    }

    @Test
    @DisplayName("ErrorCode 인터페이스 구현 검증")
    fun errorCodeInterface() {
        val errorCode: ErrorCode = AiErrorCode.AI_SERVICE_UNAVAILABLE
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, errorCode.status)
        assertEquals("Z-001", errorCode.code)
        assertEquals("AI 서비스를 일시적으로 사용할 수 없습니다.", errorCode.message)
    }
}
