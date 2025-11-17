package com.back.motionit.domain.auth.local.controller

import com.back.motionit.domain.auth.dto.AuthResponse
import com.back.motionit.domain.auth.dto.LoginRequest
import com.back.motionit.domain.auth.dto.SignupRequest
import com.back.motionit.domain.auth.local.service.LocalAuthService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@ExtendWith(MockitoExtension::class)
@DisplayName("LocalAuthController 단위 테스트")
class LocalAuthControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var localAuthService: LocalAuthService

    @InjectMocks
    private lateinit var localAuthController: LocalAuthController

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        mockMvc = MockMvcBuilders.standaloneSetup(localAuthController).build()
    }

    @Test
    @DisplayName("회원가입 성공 시 201 응답 반환")
    fun signup_Success() {
        val request = SignupRequest("test@email.com", "password123", "테스터")
        val response = AuthResponse(
            "access.token",
            "refresh.token",
            1L,
            "test@email.com",
            "테스터"
        )

        given(localAuthService.signup(any())).willReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/local/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.data.email").value("test@email.com"))
            .andExpect(jsonPath("$.data.nickname").value("테스터"))
    }

    @Test
    @DisplayName("로그인 성공 시 200 응답 반환")
    fun login_Success() {
        val request = LoginRequest("test@email.com", "password123")
        val response = AuthResponse(
            "access.token",
            "refresh.token",
            1L,
            "test@email.com",
            "테스터"
        )

        given(localAuthService.login(any())).willReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/local/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.data.accessToken").value("access.token"))
            .andExpect(jsonPath("$.data.nickname").value("테스터"))
    }

    @Test
    @DisplayName("로그아웃 성공 시 200 응답 반환")
    fun logout_Success() {
        mockMvc.perform(
            post("/api/v1/auth/local/logout")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200"))

        verify(localAuthService).logout()
    }
}
