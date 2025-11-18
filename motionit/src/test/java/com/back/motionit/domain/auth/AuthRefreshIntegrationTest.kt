package com.back.motionit.domain.auth

import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.security.jwt.JwtTokenProvider
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*
import java.util.Map

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Auth 재발급 통합 테스트 (Security Filter 포함)")
class AuthRefreshIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Test
    @DisplayName("refreshToken으로 새 accessToken 발급 성공")
    fun refreshAccessToken_Success() {

        val refreshToken = "valid.refresh.token"
        val newAccessToken = "new.access.token"

        val mockUser = createMockUser(1L, "테스터", refreshToken)

        given(jwtTokenProvider.isExpired(refreshToken)).willReturn(false)
        given(jwtTokenProvider.payloadOrNull(refreshToken))
            .willReturn(Map.of<String, Any?>("id", 1L, "nickname", "테스터"))
        given(jwtTokenProvider.generateAccessToken(any())).willReturn(newAccessToken)
        given(jwtTokenProvider.accessTokenExpiration).willReturn(3600L)

        given(userRepository.findByRefreshToken(refreshToken))
            .willReturn(Optional.of(mockUser))

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .cookie(Cookie("refreshToken", refreshToken))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("정상적으로 처리되었습니다."))
            .andExpect(jsonPath("$.data.accessToken").value(newAccessToken))
            .andExpect(jsonPath("$.data.expiresIn").value(3600))
    }

    @Test
    @DisplayName("refreshToken이 없으면 REFRESH_TOKEN_REQUIRED 응답")
    fun refreshAccessToken_NoRefreshToken() {
        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("U-109"))
            .andExpect(jsonPath("$.msg").value("Refresh Token이 필요합니다."))
    }

    @Test
    @DisplayName("refreshToken이 유효하지 않으면 REFRESH_TOKEN_INVALID 응답")
    fun refreshAccessToken_InvalidToken() {

        val invalidToken = "invalid.refresh.token"
        given(jwtTokenProvider.isExpired(invalidToken)).willReturn(false)
        given(jwtTokenProvider.payloadOrNull(invalidToken)).willReturn(null)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .cookie(Cookie("refreshToken", invalidToken))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("U-110"))
            .andExpect(jsonPath("$.msg").value("Refresh Token이 유효하지 않습니다."))
    }

    @Test
    @DisplayName("DB에 refreshToken이 존재하지 않으면 REFRESH_TOKEN_NOT_FOUND 응답")
    fun refreshAccessToken_NotFoundInDB() {

        val notFoundToken = "not.found.token"
        given(jwtTokenProvider.isExpired(notFoundToken)).willReturn(false)
        given(jwtTokenProvider.payloadOrNull(notFoundToken))
            .willReturn(Map.of<String, Any?>("id", 1L, "nickname", "테스터"))
        given(userRepository.findByRefreshToken(notFoundToken)).willReturn(Optional.empty())

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .cookie(Cookie("refreshToken", notFoundToken))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("U-111"))
            .andExpect(jsonPath("$.msg").value("Refresh Token을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("payload의 userId와 DB의 userId가 불일치하면 REFRESH_TOKEN_MISMATCH 응답")
    fun refreshAccessToken_UserIdMismatch() {

        val mismatchToken = "mismatch.token"
        val mockUser = createMockUser(1L, "테스터", mismatchToken)

        given(jwtTokenProvider.isExpired(mismatchToken)).willReturn(false)
        given(jwtTokenProvider.payloadOrNull(mismatchToken))
            .willReturn(Map.of<String, Any?>("id", 999L, "nickname", "테스터")) // 다른 userId
        given(userRepository.findByRefreshToken(mismatchToken)).willReturn(Optional.of(mockUser))

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .cookie(Cookie("refreshToken", mismatchToken))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("U-112"))
            .andExpect(jsonPath("$.msg").value("Refresh Token이 일치하지 않습니다."))
    }

    @Test
    @DisplayName("refreshToken이 만료되면 REFRESH_TOKEN_EXPIRED 응답")
    fun refreshAccessToken_ExpiredToken() {

        val expiredToken = "expired.refresh.token"
        given(jwtTokenProvider.isExpired(expiredToken)).willReturn(true)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .cookie(Cookie("refreshToken", expiredToken))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("U-113"))
            .andExpect(jsonPath("$.msg").value("Refresh Token이 만료되었습니다."))
    }

    @Test
    @DisplayName("/api/v1/auth/** 경로는 인증 없이 접근 가능")
    fun authPath_NoAuthenticationRequired() {

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").exists())
    }

    @Test
    @DisplayName("인증이 필요한 API에 accessToken 없으면 TOKEN_EXPIRED 응답")
    fun protectedApi_NoAccessToken() {

        given(jwtTokenProvider.isExpired("")).willReturn(true)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/user/me")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("U-108"))
    }

    private fun createMockUser(id: Long, nickname: String, refreshToken: String): User {
        val user = User(id, nickname)
        user.updateRefreshToken(refreshToken)
        return user
    }
}
