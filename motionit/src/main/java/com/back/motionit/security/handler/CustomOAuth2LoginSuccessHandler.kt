package com.back.motionit.security.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.back.motionit.domain.auth.social.service.SocialAuthService;
import com.back.motionit.global.request.RequestContext;
import com.back.motionit.security.SecurityUser;
import com.back.motionit.security.jwt.JwtTokenDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final SocialAuthService socialAuthService;
	private final RequestContext requestContext;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		SecurityUser securityUser = (SecurityUser)authentication.getPrincipal();
		Long userId = securityUser.getId();

		JwtTokenDto tokens = socialAuthService.generateTokensById(userId);

		requestContext.setCookie("accessToken", tokens.getAccessToken());
		requestContext.setCookie("refreshToken", tokens.getRefreshToken());

		String state = request.getParameter("state");
		String redirectUrl = "/";

		if (state != null && !state.isBlank()) {
			String decodedState = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
			redirectUrl = decodedState.split("#")[1];
		}

		requestContext.sendRedirect(redirectUrl);
	}
}
