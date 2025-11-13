package com.back.motionit.global.error.handler

import com.back.motionit.global.error.code.CommonErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.global.error.util.ErrorResponse.build
import com.back.motionit.global.respoonsedata.ResponseData
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.ConstraintViolationException

import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = KotlinLogging.logger {}
    /* =========================
	   도메인 비즈니스 예외
	   ========================= */
    @ExceptionHandler(BusinessException::class)
    @ResponseBody
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ResponseData<Void>> {
        logger.error { "BusinessException: {${ex.message}}" }
        return build(ex.errorCode)
    }

    /* =========================
	   검증/바인딩 예외
	   ========================= */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseBody
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ResponseData<Void>> {
        logger.error { "Validation failed: {${ex.message}}" }
        return build(CommonErrorCode.INVALID_INPUT_VALUE)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseBody
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ResponseData<Void>> {
        logger.error { "Constraint violation: {${ex.message}}" }
        return build(CommonErrorCode.INVALID_INPUT_VALUE)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseBody
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ResponseData<Void>> {
        logger.error { "Type mismatch: {${ex.message}}" }
        return build(CommonErrorCode.TYPE_MISMATCH)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    @ResponseBody
    fun handleMissingParam(ex: MissingServletRequestParameterException): ResponseEntity<ResponseData<Void>> {
        logger.error { "Missing param: {${ex.message}}" }
        return build(CommonErrorCode.MISSING_REQUEST_PARAMS)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseBody
    fun handleNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ResponseData<Void>> {
        logger.error { "Message not readable: {${ex.message}}" }
        return build(CommonErrorCode.BAD_REQUEST)
    }

    /* =========================
	   HTTP 관련 예외
	   ========================= */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    @ResponseBody
    fun handleMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException
    ): ResponseEntity<ResponseData<Void>> {
        logger.error { "Method not supported: {${ex.message}}" }
        return build(CommonErrorCode.METHOD_NOT_ALLOWED)
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    @ResponseBody
    fun handleMediaTypeNotSupported(ex: HttpMediaTypeNotSupportedException): ResponseEntity<ResponseData<Void>> {
        logger.error { "{${ex.message}}" }
        return build(CommonErrorCode.HTTP_MEDIA_NOT_SUPPORT)
    }

    /* =========================
	   그 외 모든 예외
	   ========================= */
    @ExceptionHandler(Exception::class)
    @ResponseBody
    fun handleException(ex: Exception): ResponseEntity<ResponseData<Void>> {
        logger.error { "Media type not supported: {${ex.message}}" }
        return build(CommonErrorCode.INTERNAL_SERVER_ERROR)
    }
}