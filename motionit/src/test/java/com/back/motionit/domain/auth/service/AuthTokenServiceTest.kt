package com.back.motionit.domain.auth.service

import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.error.code.AuthErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.global.request.RequestContext
import com.back.motionit.security.jwt.JwtTokenProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.test.context.ActiveProfiles
import java.util.*
import java.util.Map

@ExtendWith(MockitoExtension::class)
@ActiveProfiles("test")
internal class AuthTokenServiceTest {
    @InjectMocks
    private lateinit var authTokenService: AuthTokenService

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var requestContext: RequestContext

    private val validRt = "valid_refresh_token"
    private val newAt = "new_access_token"

    private fun createUser(): User {
        val user = User(7L, "테스터")
        user.updateRefreshToken(validRt)
        return user
    }

    // 1. refreshToken이 null 또는 공백일 때
    @Test
    @DisplayName("1. refreshToken이 없으면 REFRESH_TOKEN_REQUIRED 예외 발생")
    fun shouldThrowWhenRefreshTokenIsMissing() {
        val ex = assertThrows(BusinessException::class.java) {
            authTokenService.refreshAccessToken(null)
        }
        assertEquals(AuthErrorCode.REFRESH_TOKEN_REQUIRED, ex.errorCode)
    }

    // 2. refreshToken이 만료된 경우
    @Test
    @DisplayName("2. refreshToken이 만료되면 REFRESH_TOKEN_EXPIRED 예외 발생")
    fun shouldThrowWhenRefreshTokenIsExpired() {
        `when`(jwtTokenProvider.isExpired(validRt)).thenReturn(true)

        val ex = assertThrows(BusinessException::class.java) {
            authTokenService.refreshAccessToken(validRt)
        }
        assertEquals(AuthErrorCode.REFRESH_TOKEN_EXPIRED, ex.errorCode)
    }

    // 3. refreshToken이 유효하지 않은 경우(payload null)
    @Test
    @DisplayName("3. refreshToken이 유효하지 않으면 REFRESH_TOKEN_INVALID 예외 발생")
    fun shouldThrowWhenRefreshTokenIsInvalid() {
        `when`(jwtTokenProvider.isExpired(validRt)).thenReturn(false)
        `when`(jwtTokenProvider.payloadOrNull(validRt)).thenReturn(null)

        val ex = assertThrows(BusinessException::class.java) {
            authTokenService.refreshAccessToken(validRt)
        }
        assertEquals(AuthErrorCode.REFRESH_TOKEN_INVALID, ex.errorCode)
    }

    // 4. DB에 refreshToken이 존재하지 않는 경우
    @Test
    @DisplayName("4. DB에 refreshToken이 존재하지 않으면 REFRESH_TOKEN_NOT_FOUND 예외 발생")
    fun shouldThrowWhenRefreshTokenNotInDatabase() {
        `when`(jwtTokenProvider.isExpired(validRt)).thenReturn(false)
        `when`(jwtTokenProvider.payloadOrNull(validRt)).thenReturn(Map.of<String, Any?>("id", 7L))
        `when`(userRepository.findByRefreshToken(validRt)).thenReturn(Optional.empty())

        val ex = assertThrows(BusinessException::class.java) {
            authTokenService.refreshAccessToken(validRt)
        }
        assertEquals(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND, ex.errorCode)
    }

    // 5. payload의 userId와 DB의 userId가 일치하지 않는 경우
    @Test
    @DisplayName("5. payload의 userId와 DB userId가 불일치하면 REFRESH_TOKEN_MISMATCH 예외 발생")
    fun shouldThrowWhenPayloadUserIdMismatch() {
        `when`(jwtTokenProvider.isExpired(validRt)).thenReturn(false)
        `when`(jwtTokenProvider.payloadOrNull(validRt)).thenReturn(Map.of<String, Any?>("id", 999L))

        val user = createUser()
        `when`(userRepository.findByRefreshToken(validRt)).thenReturn(Optional.of(user))

        val ex = assertThrows(BusinessException::class.java) {
            authTokenService.refreshAccessToken(validRt)
        }
        assertEquals(AuthErrorCode.REFRESH_TOKEN_MISMATCH, ex.errorCode)
    }

    // 성공 케이스
    @Test
    @DisplayName("refreshToken이 유효하면 새 accessToken 발급 및 쿠키 설정")
    fun shouldRefreshAccessTokenSuccessfully() {
        `when`(jwtTokenProvider.isExpired(validRt)).thenReturn(false)
        `when`(jwtTokenProvider.payloadOrNull(validRt))
            .thenReturn(Map.of<String, Any?>("id", 7L, "nickname", "테스터"))
        `when`(jwtTokenProvider.generateAccessToken(any())).thenReturn(newAt)
        `when`(jwtTokenProvider.accessTokenExpiration).thenReturn(3600L)

        val user = createUser()
        `when`(userRepository.findByRefreshToken(validRt)).thenReturn(Optional.of(user))

        val response = authTokenService.refreshAccessToken(validRt)

        assertNotNull(response)
        assertEquals(newAt, response.accessToken)
        assertEquals(3600L, response.expiresIn)
        verify(requestContext).setCookie("accessToken", newAt)
    }
}
