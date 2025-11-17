package com.back.motionit.global.config.aws;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.back.motionit.global.service.CloudFrontCookieService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnBean(CloudFrontCookieService.class)
@RequiredArgsConstructor
public class CloudFrontCookieInterceptor implements HandlerInterceptor {

	private final CloudFrontCookieService cookieService;

	@Value("${aws.cloudfront.renew-seconds}")
	private String renewSeconds;

	@Value("${aws.cloudfront.cookie-duration}")
	private String cookieDuration;

	@Override
	public boolean preHandle(
		HttpServletRequest request,
		HttpServletResponse response,
		Object handler
	) {
		if (!"GET".equalsIgnoreCase(request.getMethod())) {
			return true;
		}

		if (!needsCookie(request)) {
			return true;
		}

		cookieService.setSignedCookies(response, Duration.ofHours(Integer.parseInt(cookieDuration)));
		return true;
	}

	private boolean needsCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return true;
		}

		String expires = null;
		String keypair = null;

		for (Cookie cookie : cookies) {
			if ("CloudFront-Expires".equalsIgnoreCase(cookie.getName())) {
				expires = cookie.getValue();
			}

			if ("CloudFront-Key-Pair-Id".equalsIgnoreCase(cookie.getName())) {
				keypair = cookie.getValue();
			}
		}

		if (expires == null || keypair == null) {
			return true;
		}

		try {
			long exp = Long.parseLong(expires);
			long now = System.currentTimeMillis();
			return (exp - now) <= Long.parseLong(renewSeconds);
		} catch (NumberFormatException e) {
			return true;
		}
	}
}
