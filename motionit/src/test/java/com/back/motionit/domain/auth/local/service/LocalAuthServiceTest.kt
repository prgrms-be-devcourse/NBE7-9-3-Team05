package com.back.motionit.domain.auth.local.service

import com.back.motionit.domain.auth.dto.LoginRequest
import com.back.motionit.domain.auth.dto.SignupRequest
import com.back.motionit.domain.auth.service.AuthTokenService
import com.back.motionit.domain.user.dto.UserLoginProjection
import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.error.code.AuthErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.global.request.RequestContext
import com.back.motionit.security.jwt.JwtTokenDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("LocalAuthService 단위 테스트")
class LocalAuthServiceTest {
    @InjectMocks
    private lateinit var localAuthService: LocalAuthService

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var authTokenService: AuthTokenService

    @Mock
    private lateinit var requestContext: RequestContext

    @Test
    @DisplayName("회원가입 성공 시 AuthResponse 반환")
    fun signup_Success() {

        val request = SignupRequest("test@email.com", "password123", "테스터")

        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword")

        val mockUser = createMockUser()

        given(userRepository.save(any())).willReturn(mockUser)

        val tokens = JwtTokenDto("Bearer", "access.token", "refresh.token", 3600L)
        given(authTokenService.generateTokens(any())).willReturn(tokens)

        val result = localAuthService.signup(request)

        assertNotNull(result)
        assertEquals("access.token", result.accessToken)
        assertEquals("refresh.token", result.refreshToken)
        assertEquals("테스터", result.nickname)
    }

    @Test
    @DisplayName("이미 존재하는 이메일이면 EMAIL_DUPLICATED 예외 발생")
    fun signup_DuplicateEmail() {
        val request = SignupRequest("dup@email.com", "pw123", "닉네임")

        given(userRepository.save(any())).willThrow(
            RuntimeException("uk_email")
        )

        val ex = assertThrows(BusinessException::class.java) {
            localAuthService.signup(request)
        }

        assertEquals(AuthErrorCode.EMAIL_DUPLICATED, ex.errorCode)
    }

    @Test
    @DisplayName("로그인 성공 시 쿠키 설정 및 AuthResponse 반환")
    fun login_Success() {
        val request = LoginRequest("test@email.com", "password123")

        val projection = UserLoginProjection(
            id = 1L,
            email = request.email,
            password = "encodedPassword",
            nickname = "테스터"
        )

        given(userRepository.findLoginUserByEmail(request.email))
            .willReturn(projection)

        val mockUser = createMockUser()
        given(userRepository.getReferenceById(1L))
            .willReturn(mockUser)

        given(passwordEncoder.matches(anyString(), anyString()))
            .willReturn(true)

        val tokens = JwtTokenDto("Bearer", "access.token", "refresh.token", 3600L)
        given(authTokenService.generateTokens(mockUser))
            .willReturn(tokens)

        val result = localAuthService.login(request)

        assertEquals("테스터", result.nickname)
        verify(requestContext).setCookie("accessToken", "access.token")
        verify(requestContext).setCookie("refreshToken", "refresh.token")
    }

    @Test
    @DisplayName("로그아웃 성공 시 쿠키 삭제 및 refreshToken 제거")
    fun logout_Success() {
        val refreshToken = "valid.refresh.token"
        val user = User(1L, "테스터")
        user.updateRefreshToken(refreshToken)

        given(requestContext.getCookieValue(eq("refreshToken"), any()))
            .willReturn(refreshToken)
        given(userRepository.findByRefreshToken(refreshToken)).willReturn(Optional.of(user))

        localAuthService.logout()

        verify(authTokenService).removeRefreshToken(user.id!!)
        verify(requestContext).deleteCookie("accessToken")
        verify(requestContext).deleteCookie("refreshToken")
    }

    @Test
    @DisplayName("refreshToken 쿠키가 없으면 예외 발생")
    fun logout_NoRefreshToken() {
        given(requestContext.getCookieValue(eq("refreshToken"), any()))
            .willReturn("")

        val ex = assertThrows(BusinessException::class.java) { localAuthService.logout() }
        assertEquals(AuthErrorCode.REFRESH_TOKEN_REQUIRED, ex.errorCode)
    }

    // Helper
    private fun createMockUser(): User {
        val user = User(1L, "테스터")
        user.email = "test@email.com"
        user.password = "encodedPassword"
        user.loginType = LoginType.LOCAL
        user.userProfile = "default.png"
        return user
    }
}
