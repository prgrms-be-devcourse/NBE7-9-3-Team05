package com.back.motionit.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.back.motionit.domain.auth.social.service.SocialAuthService;
import com.back.motionit.global.error.code.AuthErrorCode;
import com.back.motionit.global.error.exception.BusinessException;
import com.back.motionit.global.request.RequestContext;
import com.back.motionit.global.respoonsedata.ResponseData;
import com.back.motionit.security.jwt.JwtTokenProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
	private static final String BEARER_PREFIX = "Bearer ";

	private final SocialAuthService socialAuthService;
	private final RequestContext requestContext;
	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		logger.debug("CustomAuthenticationFilter called");

		addCorsHeaders(response);

		try {
			authenticate(request, response, filterChain);
		} catch (BusinessException e) {
			writeErrorResponse(response, e);
		}
	}

	private void authenticate(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		String requestUri = request.getRequestURI();

		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}

		if (!requestUri.startsWith("/api/")) {
			filterChain.doFilter(request, response);
			return;
		}

		if (requestUri.startsWith(AUTH_PATH_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		// Authorization 헤더 확인
		String headerAuthorization = requestContext.getHeader("Authorization", "");
		String accessToken = null;

		if (!headerAuthorization.isBlank() && headerAuthorization.startsWith(BEARER_PREFIX)) {
			accessToken = headerAuthorization.substring(BEARER_PREFIX.length());
		} else {
			// 헤더 없으면 쿠키에서 accessToken 가져오기 (기존 로직 그대로)
			accessToken = requestContext.getCookieValue("accessToken", "");
		}

		// 만료 확인
		if (jwtTokenProvider.isExpired(accessToken)) {
			throw new BusinessException(AuthErrorCode.TOKEN_EXPIRED);
		}

		Map<String, Object> payload = socialAuthService.payloadOrNull(accessToken);

		if (payload == null) {
			throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
		}

		long userId = ((Number)payload.get("id")).longValue();
		String nickname = (String)payload.getOrDefault("nickname", "");

		SecurityUser securityUser = new SecurityUser(
			userId,
			"",
			nickname,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);

		Authentication authentication = new UsernamePasswordAuthenticationToken(
			securityUser,
			null,
			securityUser.getAuthorities()
		);

		SecurityContextHolder.getContext().setAuthentication(authentication);

		filterChain.doFilter(request, response);
	}

	private void addCorsHeaders(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
		response.setHeader("Access-Control-Max-Age", "3600");
	}

	private void writeErrorResponse(HttpServletResponse response, BusinessException exception) throws IOException {
		ResponseData<Void> body = ResponseData.error(exception.getErrorCode());
		response.setStatus(exception.getErrorCode().getStatus().value());
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write(
			String.format("{\"resultCode\":\"%s\",\"msg\":\"%s\",\"data\":null}",
				body.getResultCode(), body.getMsg())
		);
	}
}

