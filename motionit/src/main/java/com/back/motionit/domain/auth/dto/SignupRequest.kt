package com.back.motionit.domain.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignupRequest(

    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    @field:Size(max = 100, message = "이메일은 100자 이내.")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 30, message = "비밀번호는 8~30자여야 합니다.")
    val password: String,

    @field:NotBlank(message = "닉네임은 필수입니다.")
    @field:Size(min = 3, max = 10, message = "닉네임은 3~10자여야 합니다.")
    val nickname: String

)
