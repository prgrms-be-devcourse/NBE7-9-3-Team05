package com.back.motionit.global.request;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.back.motionit.domain.user.entity.User;
import com.back.motionit.global.error.code.AuthErrorCode;
import com.back.motionit.global.error.exception.BusinessException;
import com.back.motionit.security.SecurityUser;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RequestContext {

	private final HttpServletRequest request;
	private final HttpServletResponse response;

	@Value("${cookie.domain:localhost}")
	private String cookieDomain;
	@Value("${jwt.refresh-token-expiration:1209600}")
	private long refreshTokenExpiration;
	@Value("${jwt.access-token-expiration:3600}")
	private long accessTokenExpiration;

	public User getActor() {

		return Optional.ofNullable(
				SecurityContextHolder
					.getContext()
					.getAuthentication()
			)
			.map(Authentication::getPrincipal)
			.filter(principal -> principal instanceof SecurityUser)
			.map(principal -> (SecurityUser)principal)
			.map(securityUser -> new User(
				securityUser.getId(),
				securityUser.getNickname()
			))
			.orElseThrow(() -> new BusinessException(AuthErrorCode.UNAUTHORIZED));
	}

	public void setHeader(String name, String value) {
		response.setHeader(name, value);
	}

	public String getHeader(String name, String defaultValue) {
		return Optional
			.ofNullable(request.getHeader(name))
			.filter(headerValue -> !headerValue.isBlank())
			.orElse(defaultValue);
	}

	public String getCookieValue(String name, String defaultValue) {
		return Optional
			.ofNullable(request.getCookies())
			.flatMap(
				cookies ->
					Arrays.stream(cookies)
						.filter(cookie -> cookie.getName().equals(name))
						.map(Cookie::getValue)
						.filter(value -> !value.isBlank())
						.findFirst()
			)
			.orElse(defaultValue);
	}

	public void setCookie(String name, String value) {
		if (value == null) {
			value = "";
		}

		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/");
		cookie.setHttpOnly(true);

		boolean isLocalhostDomain = cookieDomain == null
			|| cookieDomain.isBlank()
			|| "localhost".equalsIgnoreCase(cookieDomain)
			|| "127.0.0.1".equals(cookieDomain);

		if (!isLocalhostDomain) {
			cookie.setDomain(cookieDomain);
		}

		boolean secureRequest = request.isSecure();
		cookie.setSecure(secureRequest && !isLocalhostDomain);
		cookie.setAttribute("SameSite", isLocalhostDomain ? "Lax" : "Strict");

		if (value.isBlank()) {
			cookie.setMaxAge(0);
		} else {
			if ("accessToken".equals(name)) {
				cookie.setMaxAge((int)accessTokenExpiration);
			} else if ("refreshToken".equals(name)) {
				cookie.setMaxAge((int)refreshTokenExpiration);
			}
		}

		response.addCookie(cookie);
	}

	public void deleteCookie(String name) {
		setCookie(name, null);
	}

	public void sendRedirect(String url) throws IOException {
		response.sendRedirect(url);
	}
}
