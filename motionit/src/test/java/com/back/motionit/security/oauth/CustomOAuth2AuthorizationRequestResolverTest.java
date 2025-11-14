package com.back.motionit.security.oauth;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2AuthorizationRequestResolverTest {

	@Mock
	private ClientRegistrationRepository clientRegistrationRepository;

	@Test
	@DisplayName("authorizationRequest = null → null 반환")
	void resolveReturnsNull_WhenAuthorizationRequestIsNull() {

		// given
		CustomOAuth2AuthorizationRequestResolver resolver =
			new CustomOAuth2AuthorizationRequestResolver(clientRegistrationRepository);

		MockHttpServletRequest request = new MockHttpServletRequest();

		// defaultResolver().resolve() 가 null이라고 가정
		OAuth2AuthorizationRequest result =
			ReflectionTestUtils.invokeMethod(resolver, "customizeState", null, request);

		// then
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("redirectUrl 파라미터가 있으면 originState#redirectUrl 인코딩 후 state에 반영")
	void resolve_CustomState_WhenRedirectUrlProvided() {

		// given
		CustomOAuth2AuthorizationRequestResolver resolver =
			new CustomOAuth2AuthorizationRequestResolver(clientRegistrationRepository);

		ReflectionTestUtils.setField(resolver, "frontendRedirectUrl", "/default");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("redirectUrl", "/mypage");

		OAuth2AuthorizationRequest authorizationRequest =
			OAuth2AuthorizationRequest.authorizationCode()
				.authorizationUri("https://kauth.kakao.com/auth")
				.clientId("kakaoClient")
				.redirectUri("http://localhost/login/oauth2/code/kakao")
				.state("STATE123") // originState
				.build();

		// when — private customizeState() 직접 호출
		OAuth2AuthorizationRequest result = ReflectionTestUtils.invokeMethod(
			resolver,
			"customizeState",
			authorizationRequest,
			request
		);

		// then
		String expectedStateRaw = "STATE123#/mypage";
		String expectedEncoded = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(expectedStateRaw.getBytes(StandardCharsets.UTF_8));

		assertThat(result.getState()).isEqualTo(expectedEncoded);
	}

	@Test
	@DisplayName("redirectUrl 없으면 frontendRedirectUrl 사용")
	void resolve_UsesFrontendRedirectUrl_WhenRedirectUrlMissing() {

		// given
		CustomOAuth2AuthorizationRequestResolver resolver =
			new CustomOAuth2AuthorizationRequestResolver(clientRegistrationRepository);

		ReflectionTestUtils.setField(resolver, "frontendRedirectUrl", "/default-url");

		MockHttpServletRequest request = new MockHttpServletRequest();

		OAuth2AuthorizationRequest authorizationRequest =
			OAuth2AuthorizationRequest.authorizationCode()
				.authorizationUri("https://kauth.kakao.com/auth")
				.clientId("kakaoClient")
				.redirectUri("http://localhost/login/oauth2/code/kakao")
				.state("INIT")
				.build();

		// when
		OAuth2AuthorizationRequest result = ReflectionTestUtils.invokeMethod(
			resolver,
			"customizeState",
			authorizationRequest,
			request
		);

		// then
		String expectedStateRaw = "INIT#/default-url";
		String expectedEncoded = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(expectedStateRaw.getBytes(StandardCharsets.UTF_8));

		assertThat(result.getState()).isEqualTo(expectedEncoded);
	}

	@Test
	@DisplayName("originState 가 null 이면 빈 문자열 + #redirectUrl 조합으로 처리")
	void resolve_EmptyOriginState_WhenStateIsNull() {

		// given
		CustomOAuth2AuthorizationRequestResolver resolver =
			new CustomOAuth2AuthorizationRequestResolver(clientRegistrationRepository);

		ReflectionTestUtils.setField(resolver, "frontendRedirectUrl", "/fallback");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("redirectUrl", "/next");

		OAuth2AuthorizationRequest authorizationRequest =
			OAuth2AuthorizationRequest.authorizationCode()
				.authorizationUri("https://kauth.kakao.com/auth")
				.clientId("kakaoClient")
				.redirectUri("http://localhost/login/oauth2/code/kakao")
				// state null
				.build();

		// when
		OAuth2AuthorizationRequest result = ReflectionTestUtils.invokeMethod(
			resolver,
			"customizeState",
			authorizationRequest,
			request
		);

		// then
		String expectedRaw = "" + "#/next";
		String expectedEncoded = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(expectedRaw.getBytes(StandardCharsets.UTF_8));

		assertThat(result.getState()).isEqualTo(expectedEncoded);
	}
}
