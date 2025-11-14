package com.back.motionit.security.handler;

import static org.mockito.BDDMockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.back.motionit.domain.auth.social.service.SocialAuthService;
import com.back.motionit.global.request.RequestContext;
import com.back.motionit.security.SecurityUser;
import com.back.motionit.security.jwt.JwtTokenDto;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2LoginSuccessHandlerTest {

	@Mock
	private SocialAuthService socialAuthService;

	@Mock
	private RequestContext requestContext;

	@Test
	@DisplayName("OAuth2 로그인 성공 → 토큰 생성 → 쿠키 저장 → redirect 동작 검증")
	void onAuthenticationSuccess_Success() throws Exception {

		// given
		CustomOAuth2LoginSuccessHandler handler =
			new CustomOAuth2LoginSuccessHandler(socialAuthService, requestContext);

		Long userId = 99L;
		SecurityUser securityUser = new SecurityUser(
			userId,
			"pw",
			"testUser",
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);

		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		String redirectUrl = "/rooms/123";
		String encodedState = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(("randomstate#" + redirectUrl).getBytes(StandardCharsets.UTF_8));

		request.setParameter("state", encodedState);

		JwtTokenDto tokens = JwtTokenDto.builder()
			.accessToken("accessABC")
			.refreshToken("refreshXYZ")
			.build();

		given(socialAuthService.generateTokensById(userId))
			.willReturn(tokens);

		// when
		handler.onAuthenticationSuccess(request, response, authentication);

		// then

		verify(socialAuthService).generateTokensById(userId);

		verify(requestContext).setCookie("accessToken", "accessABC");
		verify(requestContext).setCookie("refreshToken", "refreshXYZ");

		verify(requestContext).sendRedirect(redirectUrl);
	}

	@Test
	@DisplayName("state 값이 없을 때 기본 '/' 로 redirect")
	void onAuthenticationSuccess_NoState_RedirectRoot() throws Exception {

		// given
		CustomOAuth2LoginSuccessHandler handler =
			new CustomOAuth2LoginSuccessHandler(socialAuthService, requestContext);

		Long userId = 10L;
		SecurityUser securityUser = new SecurityUser(
			userId,
			"",
			"testUser",
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);

		Authentication authentication =
			new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		JwtTokenDto tokens = JwtTokenDto.builder()
			.accessToken("ATK")
			.refreshToken("RTK")
			.build();

		given(socialAuthService.generateTokensById(userId))
			.willReturn(tokens);

		// when
		handler.onAuthenticationSuccess(request, response, authentication);

		// then
		verify(requestContext).sendRedirect("/");
	}
}
