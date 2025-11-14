package com.back.motionit.domain.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateUserProfileRequest {

	@Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다.")
	private String nickname;

	private String userProfile;
}
