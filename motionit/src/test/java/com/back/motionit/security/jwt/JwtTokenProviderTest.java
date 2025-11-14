package com.back.motionit.security.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.motionit.domain.user.entity.User;
import com.back.motionit.standard.ut.JwtUtil;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

	@Test
	@DisplayName("Access 토큰 생성 테스트 - JwtUtil에 올바른 값 전달되는지 검증")
	void generateAccessToken_Success() {

		// given
		JwtTokenProvider provider = new JwtTokenProvider();
		ReflectionTestUtils.setField(provider, "secret", "test-secret");
		ReflectionTestUtils.setField(provider, "accessTokenExpiration", 3600L);

		User user = new User(1L, "testUser");

		try (MockedStatic<JwtUtil.Jwt> mockedJwt = mockStatic(JwtUtil.Jwt.class)) {

			mockedJwt.when(() ->
					JwtUtil.Jwt.toString(
						"test-secret",
						3600L,
						Map.of("id", 1L, "nickname", "testUser")
					)
				)
				.thenReturn("mock-access-token");

			// when
			String token = provider.generateAccessToken(user);

			// then
			assertThat(token).isEqualTo("mock-access-token");
		}
	}

	@Test
	@DisplayName("Refresh 토큰 생성 테스트 - JwtUtil에 올바른 값 전달되는지 검증")
	void generateRefreshToken_Success() {

		// given
		JwtTokenProvider provider = new JwtTokenProvider();
		ReflectionTestUtils.setField(provider, "secret", "test-secret");
		ReflectionTestUtils.setField(provider, "refreshTokenExpiration", 7200L);

		User user = new User(1L, "testUser");

		try (MockedStatic<JwtUtil.Jwt> mockedJwt = mockStatic(JwtUtil.Jwt.class)) {

			mockedJwt.when(() ->
					JwtUtil.Jwt.toString(
						"test-secret",
						7200L,
						Map.of("id", 1L, "nickname", "testUser")
					)
				)
				.thenReturn("mock-refresh-token");

			// when
			String token = provider.generateRefreshToken(user);

			// then
			assertThat(token).isEqualTo("mock-refresh-token");
		}
	}

	@Test
	@DisplayName("payloadOrNull - JwtUtil에서 payload 반환 시 정상 변환")
	void payloadOrNull_Success() {

		JwtTokenProvider provider = new JwtTokenProvider();
		ReflectionTestUtils.setField(provider, "secret", "test-secret");

		Map<String, Object> fakePayload = Map.of(
			"id", 1,
			"nickname", "testUser"
		);

		try (MockedStatic<JwtUtil.Jwt> mockedJwt = mockStatic(JwtUtil.Jwt.class)) {

			mockedJwt.when(() -> JwtUtil.Jwt.payloadOrNull("validToken", "test-secret"))
				.thenReturn(fakePayload);

			// when
			Map<String, Object> result = provider.payloadOrNull("validToken");

			// then
			assertThat(result.get("id")).isEqualTo(1L);
			assertThat(result.get("nickname")).isEqualTo("testUser");
		}
	}

	@Test
	@DisplayName("payloadOrNull - null 반환 시 null 반환")
	void payloadOrNull_ReturnsNull() {

		JwtTokenProvider provider = new JwtTokenProvider();
		ReflectionTestUtils.setField(provider, "secret", "test-secret");

		try (MockedStatic<JwtUtil.Jwt> mockedJwt = mockStatic(JwtUtil.Jwt.class)) {

			mockedJwt.when(() -> JwtUtil.Jwt.payloadOrNull("invalidToken", "test-secret"))
				.thenReturn(null);

			// when
			Map<String, Object> result = provider.payloadOrNull("invalidToken");

			// then
			assertThat(result).isNull();
		}
	}

	@Test
	@DisplayName("isExpired - JwtUtil의 isExpired 결과를 그대로 반환")
	void isExpired_Success() {

		JwtTokenProvider provider = new JwtTokenProvider();
		ReflectionTestUtils.setField(provider, "secret", "test-secret");

		try (MockedStatic<JwtUtil.Jwt> mockedJwt = mockStatic(JwtUtil.Jwt.class)) {

			mockedJwt.when(() -> JwtUtil.Jwt.isExpired("expiredToken", "test-secret"))
				.thenReturn(true);

			// when
			boolean result = provider.isExpired("expiredToken");

			// then
			assertThat(result).isTrue();
		}
	}
}
