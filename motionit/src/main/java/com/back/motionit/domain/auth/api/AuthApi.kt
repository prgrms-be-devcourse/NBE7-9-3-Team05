package com.back.motionit.domain.auth.api

import com.back.motionit.domain.auth.dto.TokenRefreshResponse
import com.back.motionit.global.respoonsedata.ResponseData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping

@Tag(name = "인증/인가(재발급)", description = "공통 인증 API")
interface AuthApi {
    @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰으로 액세스 토큰을 재발급합니다.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "토큰 재발급 성공",
            content = [Content(schema = Schema(implementation = TokenRefreshResponse::class))]
        ), ApiResponse(responseCode = "401", description = "리프레시 토큰이 유효하지 않습니다.")
    )
    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(name = "refreshToken", required = false) refreshToken: String?
    ): ResponseData<TokenRefreshResponse>
}
