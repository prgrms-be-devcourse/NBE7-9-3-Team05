package com.back.motionit.domain.auth;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.security.jwt.JwtTokenProvider;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Auth 재발급 통합 테스트 (Security Filter 포함)")
public class AuthRefreshIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("refreshToken으로 새 accessToken 발급 성공")
	void refreshAccessToken_Success() throws Exception {
		// given
		String refreshToken = "valid.refresh.token";
		String newAccessToken = "new.access.token";

		User mockUser = createMockUser(1L, "테스터", refreshToken);

		// JwtTokenProvider Mock 설정
		given(jwtTokenProvider.isExpired(refreshToken)).willReturn(false);
		given(jwtTokenProvider.payloadOrNull(refreshToken))
			.willReturn(Map.of("id", 1L, "nickname", "테스터"));
		given(jwtTokenProvider.generateAccessToken(any(User.class)))
			.willReturn(newAccessToken);
		given(jwtTokenProvider.getAccessTokenExpiration()).willReturn(3600L);

		// UserRepository Mock 설정
		given(userRepository.findByRefreshToken(refreshToken))
			.willReturn(Optional.of(mockUser));

		// when & then
		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie("refreshToken", refreshToken))
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.resultCode").value("200"))
			.andExpect(jsonPath("$.msg").value("정상적으로 처리되었습니다."))
			.andExpect(jsonPath("$.data.accessToken").value(newAccessToken))
			.andExpect(jsonPath("$.data.expiresIn").value(3600));
	}

	@Test
	@DisplayName("refreshToken이 없으면 REFRESH_TOKEN_REQUIRED 응답")
	void refreshAccessToken_NoRefreshToken() throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.resultCode").value("U-109"))
			.andExpect(jsonPath("$.msg").value("Refresh Token이 필요합니다."));
	}

	@Test
	@DisplayName("refreshToken이 유효하지 않으면 REFRESH_TOKEN_INVALID 응답")
	void refreshAccessToken_InvalidToken() throws Exception {
		// given
		String invalidToken = "invalid.refresh.token";
		given(jwtTokenProvider.isExpired(invalidToken)).willReturn(false);
		given(jwtTokenProvider.payloadOrNull(invalidToken)).willReturn(null);

		// when & then
		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie("refreshToken", invalidToken))
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.resultCode").value("U-110"))
			.andExpect(jsonPath("$.msg").value("Refresh Token이 유효하지 않습니다."));
	}

	@Test
	@DisplayName("DB에 refreshToken이 존재하지 않으면 REFRESH_TOKEN_NOT_FOUND 응답")
	void refreshAccessToken_NotFoundInDB() throws Exception {
		// given
		String notFoundToken = "not.found.token";
		given(jwtTokenProvider.isExpired(notFoundToken)).willReturn(false);
		given(jwtTokenProvider.payloadOrNull(notFoundToken))
			.willReturn(Map.of("id", 1L, "nickname", "테스터"));
		given(userRepository.findByRefreshToken(notFoundToken))
			.willReturn(Optional.empty());

		// when & then
		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie("refreshToken", notFoundToken))
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.resultCode").value("U-111"))
			.andExpect(jsonPath("$.msg").value("Refresh Token을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("payload의 userId와 DB의 userId가 불일치하면 REFRESH_TOKEN_MISMATCH 응답")
	void refreshAccessToken_UserIdMismatch() throws Exception {
		// given
		String mismatchToken = "mismatch.token";
		User mockUser = createMockUser(1L, "테스터", mismatchToken);

		given(jwtTokenProvider.isExpired(mismatchToken)).willReturn(false);
		given(jwtTokenProvider.payloadOrNull(mismatchToken))
			.willReturn(Map.of("id", 999L, "nickname", "테스터")); // 다른 userId
		given(userRepository.findByRefreshToken(mismatchToken))
			.willReturn(Optional.of(mockUser));

		// when & then
		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie("refreshToken", mismatchToken))
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.resultCode").value("U-112"))
			.andExpect(jsonPath("$.msg").value("Refresh Token이 일치하지 않습니다."));
	}

	@Test
	@DisplayName("refreshToken이 만료되면 REFRESH_TOKEN_EXPIRED 응답")
	void refreshAccessToken_ExpiredToken() throws Exception {
		// given
		String expiredToken = "expired.refresh.token";
		given(jwtTokenProvider.isExpired(expiredToken)).willReturn(true);

		// when & then
		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie("refreshToken", expiredToken))
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.resultCode").value("U-113"))
			.andExpect(jsonPath("$.msg").value("Refresh Token이 만료되었습니다."));
	}

	@Test
	@DisplayName("/api/v1/auth/** 경로는 인증 없이 접근 가능")
	void authPath_NoAuthenticationRequired() throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isUnauthorized()) // 토큰 없어서 실패하지만 필터는 통과
			.andExpect(jsonPath("$.resultCode").exists());
	}

	@Test
	@DisplayName("인증이 필요한 API에 accessToken 없으면 TOKEN_EXPIRED 응답")
	void protectedApi_NoAccessToken() throws Exception {
		// given
		given(jwtTokenProvider.isExpired("")).willReturn(true);

		// when & then
		mockMvc.perform(get("/api/v1/user/me")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.resultCode").value("U-108"));
	}

	private User createMockUser(Long id, String nickname, String refreshToken) {
		User user = new User(id, nickname);
		user.updateRefreshToken(refreshToken);
		return user;
	}
}
