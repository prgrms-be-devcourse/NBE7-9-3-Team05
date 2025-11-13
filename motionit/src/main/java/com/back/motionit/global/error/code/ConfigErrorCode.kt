package com.back.motionit.global.error.code

import org.springframework.http.HttpStatus

enum class ConfigErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    FAILED_SIGN_CLOUD_FRONT_URL(HttpStatus.BAD_REQUEST, "S-001", "AWS CloudFront URL Sign 실패"),
    FAILED_LOAD_PRIVATE_KEY(HttpStatus.BAD_REQUEST, "S-002", "AWS private key 로드 실패")
}