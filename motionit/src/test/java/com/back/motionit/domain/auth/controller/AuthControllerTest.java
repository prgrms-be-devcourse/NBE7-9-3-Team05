package com.back.motionit.domain.auth.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.back.motionit.domain.auth.controller.AuthController;
import com.back.motionit.domain.auth.dto.TokenRefreshResponse;
import com.back.motionit.domain.auth.service.AuthTokenService;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 테스트")
public class AuthControllerTest {

	private MockMvc mockMvc;

	@Mock
	private AuthTokenService authTokenService;

	@InjectMocks
	private AuthController authController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
	}

	@Test
	@DisplayName("refreshToken 쿠키로 accessToken 재발급 성공")
	void refreshAccessToken_Success() throws Exception {
		// given
		String refreshToken = "valid.refresh.token";
		String newAccessToken = "new.access.token";
		long expiresIn = 3600L;

		TokenRefreshResponse response = TokenRefreshResponse.builder()
			.accessToken(newAccessToken)
			.expiresIn(expiresIn)
			.build();

		given(authTokenService.refreshAccessToken(refreshToken))
			.willReturn(response);

		Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);

		// when & then
		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(refreshTokenCookie))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.resultCode").value("200"))
			.andExpect(jsonPath("$.msg").value("정상적으로 처리되었습니다."))
			.andExpect(jsonPath("$.data.accessToken").value(newAccessToken))
			.andExpect(jsonPath("$.data.expiresIn").value(expiresIn));

		verify(authTokenService).refreshAccessToken(refreshToken);
	}

	@Test
	@DisplayName("refreshToken 쿠키가 없으면 null로 서비스 호출")
	void refreshAccessToken_NoCookie() throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/auth/refresh"))
			.andDo(print());

		verify(authTokenService).refreshAccessToken(null);
	}
}
