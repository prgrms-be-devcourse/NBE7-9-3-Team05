package com.back.motionit.domain.auth.controller

import com.back.motionit.domain.auth.dto.TokenRefreshResponse
import com.back.motionit.domain.auth.service.AuthTokenService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@ExtendWith(MockitoExtension::class)
@DisplayName("AuthController 테스트")
class AuthControllerTest {
    private lateinit var mockMvc: MockMvc

    @Mock
    private lateinit var authTokenService: AuthTokenService

    @InjectMocks
    private lateinit var authController: AuthController

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build()
    }

    @Test
    @DisplayName("refreshToken 쿠키로 accessToken 재발급 성공")
    fun refreshAccessToken_Success() {
        val refreshToken = "valid.refresh.token"
        val newAccessToken = "new.access.token"
        val expiresIn = 3600L

        val response = TokenRefreshResponse(
            newAccessToken,
            expiresIn
        )

        given(authTokenService.refreshAccessToken(refreshToken))
            .willReturn(response)

        val refreshTokenCookie = Cookie("refreshToken", refreshToken)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .cookie(refreshTokenCookie)
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("정상적으로 처리되었습니다."))
            .andExpect(jsonPath("$.data.accessToken").value(newAccessToken))
            .andExpect(jsonPath("$.data.expiresIn").value(expiresIn))

        verify(authTokenService).refreshAccessToken(refreshToken)
    }

    @Test
    @DisplayName("refreshToken 쿠키가 없으면 null로 서비스 호출")
    fun refreshAccessToken_NoCookie() {
        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andDo(print())

        verify(authTokenService).refreshAccessToken(null)
    }
}
