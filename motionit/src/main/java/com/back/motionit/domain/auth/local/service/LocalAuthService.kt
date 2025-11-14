package com.back.motionit.domain.auth.local.service

import com.back.motionit.domain.auth.dto.AuthResponse
import com.back.motionit.domain.auth.dto.LoginRequest
import com.back.motionit.domain.auth.dto.SignupRequest
import com.back.motionit.domain.auth.service.AuthTokenService
import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.entity.User.Companion.builder
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.constants.ProfileImageConstants
import com.back.motionit.global.error.code.AuthErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.global.request.RequestContext
import com.back.motionit.security.jwt.JwtTokenDto
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LocalAuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authTokenService: AuthTokenService,
    private val requestContext: RequestContext
) {

    @Transactional
    fun signup(request: SignupRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw BusinessException(AuthErrorCode.EMAIL_DUPLICATED)
        }

        if (userRepository.existsByNickname(request.nickname)) {
            throw BusinessException(AuthErrorCode.NICKNAME_DUPLICATED)
        }

        val encodedPassword = passwordEncoder.encode(request.password)

        val user = builder()
            .email(request.email)
            .password(encodedPassword)
            .nickname(request.nickname)
            .loginType(LoginType.LOCAL)
            .userProfile(ProfileImageConstants.DEFAULT_PROFILE_IMAGE)
            .build()

        val savedUser = userRepository.save(user)

        val tokens = authTokenService.generateTokens(savedUser)

        return buildAuthResponse(savedUser, tokens)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { BusinessException(AuthErrorCode.LOGIN_FAILED) }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw BusinessException(AuthErrorCode.LOGIN_FAILED)
        }

        val tokens = authTokenService.generateTokens(user)

        requestContext.setCookie("accessToken", tokens.accessToken)
        requestContext.setCookie("refreshToken", tokens.refreshToken)

        return buildAuthResponse(user, tokens)
    }

    @Transactional
    fun logout() {
        val refreshToken = requestContext.getCookieValue("refreshToken", null)
        if (refreshToken.isNullOrBlank()) {
            throw BusinessException(AuthErrorCode.REFRESH_TOKEN_REQUIRED)
        }

        val user = userRepository.findByRefreshToken(refreshToken)
            .orElseThrow { BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND) }

        authTokenService.removeRefreshToken(user.id)

        requestContext.deleteCookie("accessToken")
        requestContext.deleteCookie("refreshToken")
    }

    private fun buildAuthResponse(user: User, tokens: JwtTokenDto): AuthResponse {
        return AuthResponse(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            userId = user.id!!,
            email = user.email,
            nickname = user.nickname
        )
    }
}
