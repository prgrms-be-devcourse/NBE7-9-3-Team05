package com.back.motionit.domain.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.back.motionit.domain.auth.dto.TokenRefreshResponse;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.error.code.AuthErrorCode;
import com.back.motionit.global.error.exception.BusinessException;
import com.back.motionit.global.request.RequestContext;
import com.back.motionit.security.jwt.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthTokenServiceTest {

	@InjectMocks
	private AuthTokenService authTokenService;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private UserRepository userRepository;

	@Mock
	private RequestContext requestContext;

	private static final String VALID_RT = "valid_refresh_token";
	private static final String NEW_AT = "new_access_token";

	private User createUser() {
		User user = new User(7L, "테스터");
		user.updateRefreshToken(VALID_RT);
		return user;
	}

	// 1. refreshToken이 null 또는 공백일 때
	@Test
	@DisplayName("1. refreshToken이 없으면 REFRESH_TOKEN_REQUIRED 예외 발생")
	void shouldThrowWhenRefreshTokenIsMissing() {
		BusinessException ex = assertThrows(BusinessException.class, () ->
			authTokenService.refreshAccessToken(null)
		);
		assertEquals(AuthErrorCode.REFRESH_TOKEN_REQUIRED, ex.getErrorCode());
	}

	// 2. refreshToken이 만료된 경우
	@Test
	@DisplayName("2. refreshToken이 만료되면 REFRESH_TOKEN_EXPIRED 예외 발생")
	void shouldThrowWhenRefreshTokenIsExpired() {
		when(jwtTokenProvider.isExpired(VALID_RT)).thenReturn(true);

		BusinessException ex = assertThrows(BusinessException.class, () ->
			authTokenService.refreshAccessToken(VALID_RT)
		);
		assertEquals(AuthErrorCode.REFRESH_TOKEN_EXPIRED, ex.getErrorCode());
	}

	// 3. refreshToken이 유효하지 않은 경우(payload null)
	@Test
	@DisplayName("3. refreshToken이 유효하지 않으면 REFRESH_TOKEN_INVALID 예외 발생")
	void shouldThrowWhenRefreshTokenIsInvalid() {
		when(jwtTokenProvider.isExpired(VALID_RT)).thenReturn(false);
		when(jwtTokenProvider.payloadOrNull(VALID_RT)).thenReturn(null);

		BusinessException ex = assertThrows(BusinessException.class, () ->
			authTokenService.refreshAccessToken(VALID_RT)
		);
		assertEquals(AuthErrorCode.REFRESH_TOKEN_INVALID, ex.getErrorCode());
	}

	// 4. DB에 refreshToken이 존재하지 않는 경우
	@Test
	@DisplayName("4. DB에 refreshToken이 존재하지 않으면 REFRESH_TOKEN_NOT_FOUND 예외 발생")
	void shouldThrowWhenRefreshTokenNotInDatabase() {
		when(jwtTokenProvider.isExpired(VALID_RT)).thenReturn(false);
		when(jwtTokenProvider.payloadOrNull(VALID_RT)).thenReturn(Map.of("id", 7L));
		when(userRepository.findByRefreshToken(VALID_RT)).thenReturn(Optional.empty());

		BusinessException ex = assertThrows(BusinessException.class, () ->
			authTokenService.refreshAccessToken(VALID_RT)
		);
		assertEquals(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND, ex.getErrorCode());
	}

	// 5. payload의 userId와 DB의 userId가 일치하지 않는 경우
	@Test
	@DisplayName("5. payload의 userId와 DB userId가 불일치하면 REFRESH_TOKEN_MISMATCH 예외 발생")
	void shouldThrowWhenPayloadUserIdMismatch() {
		when(jwtTokenProvider.isExpired(VALID_RT)).thenReturn(false);
		when(jwtTokenProvider.payloadOrNull(VALID_RT)).thenReturn(Map.of("id", 999L));

		User user = createUser();
		when(userRepository.findByRefreshToken(VALID_RT)).thenReturn(Optional.of(user));

		BusinessException ex = assertThrows(BusinessException.class, () ->
			authTokenService.refreshAccessToken(VALID_RT)
		);
		assertEquals(AuthErrorCode.REFRESH_TOKEN_MISMATCH, ex.getErrorCode());
	}

	// 성공 케이스
	@Test
	@DisplayName("refreshToken이 유효하면 새 accessToken 발급 및 쿠키 설정")
	void shouldRefreshAccessTokenSuccessfully() {
		when(jwtTokenProvider.isExpired(VALID_RT)).thenReturn(false);
		when(jwtTokenProvider.payloadOrNull(VALID_RT)).thenReturn(Map.of("id", 7L, "nickname", "테스터"));
		when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn(NEW_AT);
		when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600L);

		User user = createUser();
		when(userRepository.findByRefreshToken(VALID_RT)).thenReturn(Optional.of(user));

		TokenRefreshResponse response = authTokenService.refreshAccessToken(VALID_RT);

		assertNotNull(response);
		assertEquals(NEW_AT, response.getAccessToken());
		assertEquals(3600L, response.getExpiresIn());
		verify(requestContext).setCookie("accessToken", NEW_AT);
	}
}
