package com.back.motionit.security.oauth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.test.util.ReflectionTestUtils.invokeMethod
import org.springframework.test.util.ReflectionTestUtils.setField
import java.nio.charset.StandardCharsets
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class CustomOAuth2AuthorizationRequestResolverTest {

    @Mock
    private lateinit var clientRegistrationRepository: ClientRegistrationRepository

    @Test
    @DisplayName("authorizationRequest = null → null 반환")
    fun resolveReturnsNull_WhenAuthorizationRequestIsNull() {

        val resolver =
            CustomOAuth2AuthorizationRequestResolver(clientRegistrationRepository)

        val request = MockHttpServletRequest()

        val result =
            invokeMethod<OAuth2AuthorizationRequest>(resolver, "customizeState", null, request)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("redirectUrl 파라미터가 있으면 originState#redirectUrl 인코딩 후 state에 반영")
    fun resolve_CustomState_WhenRedirectUrlProvided() {

        val resolver =
            CustomOAuth2AuthorizationRequestResolver(clientRegistrationRepository)

        setField(resolver, "frontendRedirectUrl", "/default")

        val request = MockHttpServletRequest().apply {
            setParameter("redirectUrl", "/mypage")
        }

        val authorizationRequest =
            OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.kakao.com/auth")
                .clientId("kakaoClient")
                .redirectUri("http://localhost/login/oauth2/code/kakao")
                .state("STATE123") // originState
                .build()

        val result = invokeMethod<OAuth2AuthorizationRequest>(
            resolver,
            "customizeState",
            authorizationRequest,
            request
        )

        val expectedStateRaw = "STATE123#/mypage"
        val expectedEncoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(expectedStateRaw.toByteArray(StandardCharsets.UTF_8))

        assertThat(result!!.state).isEqualTo(expectedEncoded)
    }

    @Test
    @DisplayName("redirectUrl 없으면 frontendRedirectUrl 사용")
    fun resolve_UsesFrontendRedirectUrl_WhenRedirectUrlMissing() {

        val resolver =
            CustomOAuth2AuthorizationRequestResolver(clientRegistrationRepository)

        setField(resolver, "frontendRedirectUrl", "/default-url")

        val request = MockHttpServletRequest()

        val authorizationRequest =
            OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.kakao.com/auth")
                .clientId("kakaoClient")
                .redirectUri("http://localhost/login/oauth2/code/kakao")
                .state("INIT")
                .build()

        val result = invokeMethod<OAuth2AuthorizationRequest>(
            resolver,
            "customizeState",
            authorizationRequest,
            request
        )

        val expectedStateRaw = "INIT#/default-url"
        val expectedEncoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(expectedStateRaw.toByteArray(StandardCharsets.UTF_8))

        assertThat(result!!.state).isEqualTo(expectedEncoded)
    }

    @Test
    @DisplayName("originState 가 null 이면 빈 문자열 + #redirectUrl 조합으로 처리")
    fun resolve_EmptyOriginState_WhenStateIsNull() {

        val resolver =
            CustomOAuth2AuthorizationRequestResolver(clientRegistrationRepository)

        setField(resolver, "frontendRedirectUrl", "/fallback")

        val request = MockHttpServletRequest().apply {
            setParameter("redirectUrl", "/next")
        }

        val authorizationRequest =
            OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.kakao.com/auth")
                .clientId("kakaoClient")
                .redirectUri("http://localhost/login/oauth2/code/kakao")
                .build()

        val result = invokeMethod<OAuth2AuthorizationRequest>(
            resolver,
            "customizeState",
            authorizationRequest,
            request
        )

        val expectedRaw = "" + "#/next"
        val expectedEncoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(expectedRaw.toByteArray(StandardCharsets.UTF_8))

        assertThat(result!!.state).isEqualTo(expectedEncoded)
    }
}
