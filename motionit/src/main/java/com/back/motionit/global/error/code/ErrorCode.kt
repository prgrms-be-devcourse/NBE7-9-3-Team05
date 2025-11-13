package com.back.motionit.global.error.code

import org.springframework.http.HttpStatus

interface ErrorCode {
    val message: String
    val status: HttpStatus
    val code: String
}