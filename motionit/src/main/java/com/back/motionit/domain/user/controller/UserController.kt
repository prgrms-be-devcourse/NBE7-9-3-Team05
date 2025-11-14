package com.back.motionit.domain.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.motionit.domain.user.dto.UpdateUserProfileRequest;
import com.back.motionit.domain.user.dto.UserProfileResponse;
import com.back.motionit.domain.user.service.UserService;
import com.back.motionit.global.request.RequestContext;
import com.back.motionit.global.respoonsedata.ResponseData;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final RequestContext requestContext;

	@GetMapping("/profile")
	public ResponseData<UserProfileResponse> getUserProfile() {
		Long userId = requestContext.getActor().getId();
		UserProfileResponse profile = userService.getUserProfile(userId);
		return ResponseData.success(profile);
	}

	@PutMapping("/profile")
	public ResponseData<UserProfileResponse> updateProfile(
		@Valid @RequestBody UpdateUserProfileRequest request) {
		Long userId = requestContext.getActor().getId();
		UserProfileResponse response = userService.updateUserProfile(userId, request);
		return ResponseData.success(response);
	}
}
