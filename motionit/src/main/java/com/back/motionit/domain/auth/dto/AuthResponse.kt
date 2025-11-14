package com.back.motionit.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
	private String accessToken;
	private String refreshToken;
	private Long userId;
	private String email;
	private String nickname;
}
