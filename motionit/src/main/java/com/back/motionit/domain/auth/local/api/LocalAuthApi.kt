package com.back.motionit.domain.auth.local.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.back.motionit.domain.auth.dto.AuthResponse;
import com.back.motionit.domain.auth.dto.LoginRequest;
import com.back.motionit.domain.auth.dto.SignupRequest;
import com.back.motionit.global.respoonsedata.ResponseData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "인증/인가", description = "일반 로그인 API")
public interface LocalAuthApi {

	@Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "회원가입 성공",
			content = @Content(schema = @Schema(implementation = AuthResponse.class))),
		@ApiResponse(responseCode = "400", description = "유효하지 않은 입력값 (C-002)"),
		@ApiResponse(responseCode = "409", description = "이메일 중복 (U-100) 또는 닉네임 중복 (U-101)")
	})
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	ResponseData<AuthResponse> signup(@Valid @RequestBody SignupRequest request);

	@Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그인 성공",
			content = @Content(schema = @Schema(implementation = AuthResponse.class))),
		@ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치 (U-102)")
	})
	@PostMapping("/login")
	ResponseData<AuthResponse> login(@Valid @RequestBody LoginRequest request);

	@Operation(summary = "로그아웃", description = "리프레시 토큰을 제거하여 로그아웃합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그아웃 성공"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음 (U-103)")
	})
	@PostMapping("/logout")
	ResponseData<Void> logout();
}
