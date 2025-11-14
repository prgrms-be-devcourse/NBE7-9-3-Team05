package com.back.motionit.domain.auth.local.controller

import com.back.motionit.domain.auth.dto.AuthResponse
import com.back.motionit.domain.auth.dto.LoginRequest
import com.back.motionit.domain.auth.dto.SignupRequest
import com.back.motionit.domain.auth.local.api.LocalAuthApi
import com.back.motionit.domain.auth.local.service.LocalAuthService
import com.back.motionit.global.respoonsedata.ResponseData
import com.back.motionit.global.respoonsedata.ResponseData.Companion.success
import jakarta.validation.Valid
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth/local")
@Validated
class LocalAuthController(
    private val localAuthService: LocalAuthService
) : LocalAuthApi {

    override fun signup(
        @Valid @RequestBody request: SignupRequest
    ): ResponseData<AuthResponse> =
        success(localAuthService.signup(request))

    override fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseData<AuthResponse> =
        success(localAuthService.login(request))

    override fun logout(): ResponseData<Void> {
        localAuthService.logout()
        return success(null)
    }
}
