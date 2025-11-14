package com.back.motionit.security.jwt;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.back.motionit.domain.user.entity.User;
import com.back.motionit.standard.ut.JwtUtil;

import lombok.Getter;

@Component
@Getter
public class JwtTokenProvider {
	@Value("${jwt.secret}")
	private String secret;
	@Value("${jwt.access-token-expiration}")
	private long accessTokenExpiration;
	@Value("${jwt.refresh-token-expiration}")
	private long refreshTokenExpiration;

	public String generateAccessToken(User user) {

		return JwtUtil.Jwt.toString(
			secret,
			accessTokenExpiration,
			Map.of("id", user.getId(), "nickname", user.getNickname())
		);
	}

	public String generateRefreshToken(User user) {

		return JwtUtil.Jwt.toString(
			secret,
			refreshTokenExpiration,
			Map.of("id", user.getId(), "nickname", user.getNickname())
		);
	}

	public Map<String, Object> payloadOrNull(String jwt) {
		Map<String, Object> payload = JwtUtil.Jwt.payloadOrNull(jwt, secret);

		if (payload == null) {
			return null;
		}

		Number idNo = (Number)payload.get("id");
		long id = idNo.longValue();

		String nickname = (String)payload.get("nickname");

		return Map.of("id", id, "nickname", nickname);
	}

	public boolean isExpired(String jwt) {
		return JwtUtil.Jwt.isExpired(jwt, secret);
	}
}
