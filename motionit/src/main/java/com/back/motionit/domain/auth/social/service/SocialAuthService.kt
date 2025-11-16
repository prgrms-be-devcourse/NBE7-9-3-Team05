package com.back.motionit.domain.auth.social.service;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.motionit.domain.auth.service.AuthTokenService;
import com.back.motionit.domain.user.entity.LoginType;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.error.code.AuthErrorCode;
import com.back.motionit.global.error.exception.BusinessException;
import com.back.motionit.security.jwt.JwtTokenDto;
import com.back.motionit.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthTokenService authTokenService;
	private final JwtTokenProvider jwtTokenProvider;

	public User join(Long kakaoId, String email, String nickname, String password, LoginType loginType,
		String userProfile) {

		userRepository.findByNickname(nickname)
			.ifPresent(m -> {
				throw new BusinessException(AuthErrorCode.NICKNAME_DUPLICATED);
			});

		String encodedPassword = (password == null || password.isBlank()) ? null : passwordEncoder.encode(password);
		User user = new User(kakaoId, email, nickname, encodedPassword, loginType, userProfile);
		return userRepository.save(user);

	}

	public User modifyOrJoin(Long kakaoId, String email, String nickname, String password, LoginType loginType,
		String userProfile) {

		User user = userRepository.findByKakaoId(kakaoId).orElse(null);

		if (user == null) {
			return join(kakaoId, email, nickname, password, loginType, userProfile);
		}

		user.update(nickname, userProfile);

		return user;
	}

	@Transactional
	public JwtTokenDto generateTokensById(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
		return authTokenService.generateTokens(user);
	}

	public Map<String, Object> payloadOrNull(String accessToken) {
		return jwtTokenProvider.payloadOrNull(accessToken);
	}

}

