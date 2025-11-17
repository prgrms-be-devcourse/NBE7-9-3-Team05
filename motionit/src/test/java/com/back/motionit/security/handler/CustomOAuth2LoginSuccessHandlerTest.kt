package com.back.motionit.security.handler

import com.back.motionit.domain.auth.social.service.SocialAuthService
import com.back.motionit.global.request.RequestContext
import com.back.motionit.security.SecurityUser
import com.back.motionit.security.jwt.JwtTokenDto.Companion.builder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.nio.charset.StandardCharsets
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class CustomOAuth2LoginSuccessHandlerTest {
    @Mock
    private lateinit var socialAuthService: SocialAuthService

    @Mock
    private lateinit var requestContext: RequestContext

    @Test
    @DisplayName("OAuth2 로그인 성공 → 토큰 생성 → 쿠키 저장 → redirect 동작 검증")
    fun onAuthenticationSuccess_Success() {

        val handler =
            CustomOAuth2LoginSuccessHandler(socialAuthService, requestContext)

        val userId = 99L
        val securityUser = SecurityUser(
            userId,
            "pw",
            "testUser",
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        val authentication: Authentication =
            UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)

        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        val redirectUrl = "/rooms/123"
        val encodedState = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("randomstate#$redirectUrl".toByteArray(StandardCharsets.UTF_8))

        request.setParameter("state", encodedState)

        val tokens = builder()
            .grantType("Bearer")
            .accessToken("accessABC")
            .refreshToken("refreshXYZ")
            .accessTokenExpiresIn(3600L)
            .build()

        given(socialAuthService.generateTokensById(userId)).willReturn(tokens)

        handler.onAuthenticationSuccess(request, response, authentication)

        verify(socialAuthService).generateTokensById(userId)
        verify(requestContext).setCookie("accessToken", "accessABC")
        verify(requestContext).setCookie("refreshToken", "refreshXYZ")
        verify(requestContext).sendRedirect(redirectUrl)
    }

    @Test
    @DisplayName("state 값이 없을 때 기본 '/' 로 redirect")
    fun onAuthenticationSuccess_NoState_RedirectRoot() {

        val handler =
            CustomOAuth2LoginSuccessHandler(socialAuthService, requestContext)

        val userId = 10L
        val securityUser = SecurityUser(
            userId,
            "",
            "testUser",
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        val authentication: Authentication =
            UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)

        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        val tokens = builder()
            .grantType("Bearer")
            .accessToken("ATK")
            .refreshToken("RTK")
            .accessTokenExpiresIn(3600L)
            .build()

        given(socialAuthService.generateTokensById(userId)).willReturn(tokens)

        handler.onAuthenticationSuccess(request, response, authentication)

        verify(requestContext).sendRedirect("/")
    }
}
