package com.back.motionit.domain.auth.local.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.back.motionit.domain.auth.dto.AuthResponse;
import com.back.motionit.domain.auth.dto.LoginRequest;
import com.back.motionit.domain.auth.dto.SignupRequest;
import com.back.motionit.domain.auth.local.service.LocalAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthController 단위 테스트")
public class LocalAuthControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@Mock
	private LocalAuthService localAuthService;

	@InjectMocks
	private LocalAuthController localAuthController;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		mockMvc = MockMvcBuilders.standaloneSetup(localAuthController).build();
	}

	@Test
	@DisplayName("회원가입 성공 시 201 응답 반환")
	void signup_Success() throws Exception {
		// given
		SignupRequest request = new SignupRequest("test@email.com", "password123", "테스터");
		AuthResponse response = AuthResponse.builder()
			.accessToken("access.token")
			.refreshToken("refresh.token")
			.userId(1L)
			.email("test@email.com")
			.nickname("테스터")
			.build();

		given(localAuthService.signup(any(SignupRequest.class))).willReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.resultCode").value("200"))
			.andExpect(jsonPath("$.data.email").value("test@email.com"))
			.andExpect(jsonPath("$.data.nickname").value("테스터"));
	}

	@Test
	@DisplayName("로그인 성공 시 200 응답 반환")
	void login_Success() throws Exception {
		// given
		LoginRequest request = new LoginRequest("test@email.com", "password123");
		AuthResponse response = AuthResponse.builder()
			.accessToken("access.token")
			.refreshToken("refresh.token")
			.userId(1L)
			.email("test@email.com")
			.nickname("테스터")
			.build();

		given(localAuthService.login(any(LoginRequest.class))).willReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/auth/local/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.resultCode").value("200"))
			.andExpect(jsonPath("$.data.accessToken").value("access.token"))
			.andExpect(jsonPath("$.data.nickname").value("테스터"));
	}

	@Test
	@DisplayName("로그아웃 성공 시 200 응답 반환")
	void logout_Success() throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/auth/local/logout")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.resultCode").value("200"));

		verify(localAuthService).logout();
	}
}
