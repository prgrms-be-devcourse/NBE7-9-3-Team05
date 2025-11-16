package com.back.motionit.domain.auth.controller

import com.back.motionit.domain.auth.api.AuthApi
import com.back.motionit.domain.auth.dto.TokenRefreshResponse
import com.back.motionit.domain.auth.service.AuthTokenService
import com.back.motionit.global.respoonsedata.ResponseData
import com.back.motionit.global.respoonsedata.ResponseData.Companion.success
import lombok.RequiredArgsConstructor
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
class AuthController(
    private val authTokenService: AuthTokenService
) : AuthApi {

    override fun refresh(refreshToken: String?): ResponseData<TokenRefreshResponse> {
        val response = authTokenService.refreshAccessToken(refreshToken)
        return success(response)
    }
}
