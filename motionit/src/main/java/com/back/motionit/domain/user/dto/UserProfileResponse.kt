package com.back.motionit.domain.user.dto;

import com.back.motionit.domain.user.entity.LoginType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {

	private Long userId;
	private String email;
	private String nickname;
	private String userProfileUrl;
	private LoginType loginType;
}
