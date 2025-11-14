package com.back.motionit.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Size(max = 100, message = "이메일은 100자 이내.")
	private String email;

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 8, max = 30, message = "비밀번호는 8~30자여야 합니다.")
	private String password;

	@NotBlank(message = "닉네임은 필수입니다.")
	@Size(min = 3, max = 10, message = "닉네임은 3~10자여야 합니다.")
	private String nickname;
}
