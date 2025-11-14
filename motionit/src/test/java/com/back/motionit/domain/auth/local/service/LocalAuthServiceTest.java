package com.back.motionit.domain.auth.local.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.back.motionit.domain.auth.dto.AuthResponse;
import com.back.motionit.domain.auth.dto.LoginRequest;
import com.back.motionit.domain.auth.dto.SignupRequest;
import com.back.motionit.domain.auth.service.AuthTokenService;
import com.back.motionit.domain.user.entity.LoginType;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.error.code.AuthErrorCode;
import com.back.motionit.global.error.exception.BusinessException;
import com.back.motionit.global.request.RequestContext;
import com.back.motionit.security.jwt.JwtTokenDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthService 단위 테스트")
public class LocalAuthServiceTest {

	@InjectMocks
	private LocalAuthService localAuthService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthTokenService authTokenService;

	@Mock
	private RequestContext requestContext;

	@Test
	@DisplayName("회원가입 성공 시 AuthResponse 반환")
	void signup_Success() {
		// given
		SignupRequest request = new SignupRequest("test@email.com", "password123", "테스터");

		given(userRepository.existsByEmail(anyString())).willReturn(false);
		given(userRepository.existsByNickname(anyString())).willReturn(false);
		given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

		User mockUser = User.builder()
			.email("test@email.com")
			.password("encodedPassword")
			.nickname("테스터")
			.loginType(LoginType.LOCAL)
			.userProfile("default.png")
			.build();

		given(userRepository.save(any(User.class))).willReturn(mockUser);

		JwtTokenDto tokens = new JwtTokenDto("Bearer", "access.token", "refresh.token", 3600L);
		given(authTokenService.generateTokens(any(User.class))).willReturn(tokens);

		// when
		AuthResponse result = localAuthService.signup(request);

		// then
		assertNotNull(result);
		assertEquals("access.token", result.getAccessToken());
		assertEquals("refresh.token", result.getRefreshToken());
		assertEquals("테스터", result.getNickname());
	}

	@Test
	@DisplayName("이미 존재하는 이메일이면 EMAIL_DUPLICATED 예외 발생")
	void signup_DuplicateEmail() {
		SignupRequest request = new SignupRequest("dup@email.com", "pw123", "닉네임");
		given(userRepository.existsByEmail(anyString())).willReturn(true);

		BusinessException ex = assertThrows(BusinessException.class,
			() -> localAuthService.signup(request));

		assertEquals(AuthErrorCode.EMAIL_DUPLICATED, ex.getErrorCode());
	}

	@Test
	@DisplayName("로그인 성공 시 쿠키 설정 및 AuthResponse 반환")
	void login_Success() {
		LoginRequest request = new LoginRequest("test@email.com", "password123");
		User mockUser = User.builder()
			.email("test@email.com")
			.password("encodedPassword")
			.nickname("테스터")
			.loginType(LoginType.LOCAL)
			.build();

		given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(mockUser));
		given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

		JwtTokenDto tokens = new JwtTokenDto("Bearer", "access.token", "refresh.token", 3600L);
		given(authTokenService.generateTokens(mockUser)).willReturn(tokens);

		AuthResponse result = localAuthService.login(request);

		assertEquals("테스터", result.getNickname());
		verify(requestContext).setCookie("accessToken", "access.token");
		verify(requestContext).setCookie("refreshToken", "refresh.token");
	}

	@Test
	@DisplayName("로그아웃 성공 시 쿠키 삭제 및 refreshToken 제거")
	void logout_Success() {
		String refreshToken = "valid.refresh.token";
		User user = new User(1L, "테스터");
		user.updateRefreshToken(refreshToken);

		given(requestContext.getCookieValue(eq("refreshToken"), any())).willReturn(refreshToken);
		given(userRepository.findByRefreshToken(refreshToken)).willReturn(Optional.of(user));

		localAuthService.logout();

		verify(authTokenService).removeRefreshToken(user.getId());
		verify(requestContext).deleteCookie("accessToken");
		verify(requestContext).deleteCookie("refreshToken");
	}

	@Test
	@DisplayName("refreshToken 쿠키가 없으면 예외 발생")
	void logout_NoRefreshToken() {
		given(requestContext.getCookieValue(eq("refreshToken"), any())).willReturn(null);

		BusinessException ex = assertThrows(BusinessException.class, () -> localAuthService.logout());
		assertEquals(AuthErrorCode.REFRESH_TOKEN_REQUIRED, ex.getErrorCode());
	}
}
