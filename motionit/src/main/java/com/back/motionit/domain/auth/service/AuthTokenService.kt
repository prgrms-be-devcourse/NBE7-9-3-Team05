package com.back.motionit.domain.auth.service

import com.back.motionit.domain.auth.dto.TokenRefreshResponse
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.error.code.AuthErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.global.request.RequestContext
import com.back.motionit.security.jwt.JwtTokenDto
import com.back.motionit.security.jwt.JwtTokenDto.Companion.builder
import com.back.motionit.security.jwt.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthTokenService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val requestContext: RequestContext
) {

    @Transactional
    fun generateTokens(user: User): JwtTokenDto {
        val accessToken = jwtTokenProvider.generateAccessToken(user)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user)

        user.updateRefreshToken(refreshToken)

        return builder()
            .grantType("Bearer")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .accessTokenExpiresIn(System.currentTimeMillis())
            .build()
    }

    @Transactional
    fun removeRefreshToken(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(AuthErrorCode.USER_NOT_FOUND) }
        user.removeRefreshToken()
    }

    fun refreshAccessToken(refreshToken: String?): TokenRefreshResponse {
        // 1. refreshToken 존재 여부 확인
        if (refreshToken.isNullOrBlank()) {
            throw BusinessException(AuthErrorCode.REFRESH_TOKEN_REQUIRED)
        }

        // 2. refreshToken 만료 확인
        if (jwtTokenProvider.isExpired(refreshToken)) {
            throw BusinessException(AuthErrorCode.REFRESH_TOKEN_EXPIRED)
        }

        // 3. refreshToken 검증 및 payload 추출
        val payload = jwtTokenProvider.payloadOrNull(refreshToken)
            ?: throw BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID)

        // 4. DB에 저장된 refreshToken과 일치하는지 확인
        val user = userRepository.findByRefreshToken(refreshToken)
            .orElseThrow { BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND) }

        // 5. payload의 userId와 DB의 userId 일치 확인
        val tokenUserId = (payload["id"] as Number).toLong()
        if (user.id != tokenUserId) {
            throw BusinessException(AuthErrorCode.REFRESH_TOKEN_MISMATCH)
        }

        val newAccessToken = jwtTokenProvider.generateAccessToken(user)

        requestContext.setCookie("accessToken", newAccessToken)

        return TokenRefreshResponse(
            accessToken = newAccessToken,
            expiresIn = jwtTokenProvider.accessTokenExpiration
        )
    }
}
