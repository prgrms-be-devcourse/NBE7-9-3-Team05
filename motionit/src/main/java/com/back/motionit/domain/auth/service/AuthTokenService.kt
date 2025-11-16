package com.back.motionit.domain.auth.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.motionit.domain.auth.dto.TokenRefreshResponse;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.error.code.AuthErrorCode;
import com.back.motionit.global.error.exception.BusinessException;
import com.back.motionit.global.request.RequestContext;
import com.back.motionit.security.jwt.JwtTokenDto;
import com.back.motionit.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;
	private final RequestContext requestContext;

	@Transactional
	public JwtTokenDto generateTokens(User user) {
		String accessToken = jwtTokenProvider.generateAccessToken(user);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user);

		user.updateRefreshToken(refreshToken);

		return JwtTokenDto.builder()
			.grantType("Bearer")
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.accessTokenExpiresIn(System.currentTimeMillis())
			.build();
	}

	@Transactional
	public void removeRefreshToken(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
		user.removeRefreshToken();
	}

	@Transactional
	public TokenRefreshResponse refreshAccessToken(String refreshToken) {
		// 1. refreshToken 존재 여부 확인
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_REQUIRED);
		}

		// 2. refreshToken 만료 확인
		if (jwtTokenProvider.isExpired(refreshToken)) {
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
		}

		// 3. refreshToken 검증 및 payload 추출
		Map<String, Object> payload = jwtTokenProvider.payloadOrNull(refreshToken);

		if (payload == null) {
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
		}

		// 4. DB에 저장된 refreshToken과 일치하는지 확인
		User user = userRepository.findByRefreshToken(refreshToken)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

		// 5. payload의 userId와 DB의 userId 일치 확인
		long tokenUserId = ((Number)payload.get("id")).longValue();
		if (!user.getId().equals(tokenUserId)) {
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
		}

		String newAccessToken = jwtTokenProvider.generateAccessToken(user);

		requestContext.setCookie("accessToken", newAccessToken);

		return TokenRefreshResponse.builder()
			.accessToken(newAccessToken)
			.expiresIn(jwtTokenProvider.getAccessTokenExpiration())
			.build();
	}
}
