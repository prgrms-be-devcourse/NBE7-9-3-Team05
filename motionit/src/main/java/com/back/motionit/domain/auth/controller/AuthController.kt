package com.back.motionit.domain.auth.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.motionit.domain.auth.api.AuthApi;
import com.back.motionit.domain.auth.dto.TokenRefreshResponse;
import com.back.motionit.domain.auth.service.AuthTokenService;
import com.back.motionit.global.respoonsedata.ResponseData;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController implements AuthApi {

	private final AuthTokenService authTokenService;

	@Override
	public ResponseData<TokenRefreshResponse> refresh(String refreshToken) {
		TokenRefreshResponse response = authTokenService.refreshAccessToken(refreshToken);
		return ResponseData.success(response);
	}
}
