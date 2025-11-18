package com.back.motionit.domain.auth.social.service

import com.back.motionit.domain.auth.service.AuthTokenService
import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.error.code.AuthErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.security.jwt.JwtTokenDto
import com.back.motionit.security.jwt.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SocialAuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authTokenService: AuthTokenService,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun join(
        kakaoId: Long,
        email: String?,
        nickname: String,
        password: String?,
        loginType: LoginType,
        userProfile: String
    ): User {

        if (userRepository.existsByNickname(nickname)) {
            throw BusinessException(AuthErrorCode.NICKNAME_DUPLICATED)
        }

        val encodedPassword = if (password.isNullOrBlank()) null else passwordEncoder.encode(password)
        val user = User(
            kakaoId = kakaoId,
            email = email,
            nickname = nickname,
            password = encodedPassword,
            loginType = loginType,
            userProfile = userProfile)
        return userRepository.save(user)
    }

    @Transactional
    fun modifyOrJoin(
        kakaoId: Long,
        email: String?,
        nickname: String,
        password: String?,
        loginType: LoginType,
        userProfile: String
    ): User {

        val user = userRepository.findByKakaoId(kakaoId).orElse(null)
            ?: return join(kakaoId, email, nickname, password, loginType, userProfile)

        user.update(nickname, userProfile)

        return user
    }

    @Transactional
    fun generateTokensById(userId: Long): JwtTokenDto {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(AuthErrorCode.USER_NOT_FOUND) }
        return authTokenService.generateTokens(user)
    }

    fun payloadOrNull(accessToken: String?): Map<String, Any?>? {
        return jwtTokenProvider.payloadOrNull(accessToken)
    }
}

