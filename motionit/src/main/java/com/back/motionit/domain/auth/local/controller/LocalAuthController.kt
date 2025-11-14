package com.back.motionit.domain.auth.local.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.motionit.domain.auth.dto.AuthResponse;
import com.back.motionit.domain.auth.dto.LoginRequest;
import com.back.motionit.domain.auth.dto.SignupRequest;
import com.back.motionit.domain.auth.local.api.LocalAuthApi;
import com.back.motionit.domain.auth.local.service.LocalAuthService;
import com.back.motionit.global.respoonsedata.ResponseData;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth/local")
@RequiredArgsConstructor
@Validated
public class LocalAuthController implements LocalAuthApi {

	private final LocalAuthService localAuthService;

	@Override
	public ResponseData<AuthResponse> signup(SignupRequest request) {
		AuthResponse response = localAuthService.signup(request);
		return ResponseData.success(response);
	}

	@Override
	public ResponseData<AuthResponse> login(LoginRequest request) {
		AuthResponse response = localAuthService.login(request);
		return ResponseData.success(response);
	}

	@Override
	public ResponseData<Void> logout() {
		localAuthService.logout();
		return ResponseData.success(null);
	}
}
