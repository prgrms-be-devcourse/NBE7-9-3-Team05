package com.back.motionit.global.respoonsedata

import com.back.motionit.global.error.code.ErrorCode
import com.fasterxml.jackson.annotation.JsonIgnore

data class ResponseData<T>(
    val resultCode: String,
    val msg: String,
    val data: T? = null
) {
    @get:JsonIgnore
    val statusCode: Int
        get() =
        runCatching {
            if(resultCode.contains("-")){
                resultCode.split("-")[1].toInt()
            } else {
                resultCode.toInt()
            }
        }.getOrElse { 500 }

    companion object {

        @JvmStatic
        fun <T> success(code: String, message: String, data: T?): ResponseData<T> =
            ResponseData(code, message, data)

        @JvmStatic
        fun <T> success(message: String, data: T?): ResponseData<T> =
            ResponseData("200", message, data)

        @JvmStatic
        fun <T> success(data: T?): ResponseData<T> =
            ResponseData("200", "정상적으로 처리되었습니다.", data)

        @JvmStatic
        fun <T> error(errorCode: ErrorCode): ResponseData<T> =
            ResponseData(errorCode.code, errorCode.message, null)

        @JvmStatic
        fun <T> error(code: String, message: String): ResponseData<T> =
            ResponseData(code, message, null)
    }
}
