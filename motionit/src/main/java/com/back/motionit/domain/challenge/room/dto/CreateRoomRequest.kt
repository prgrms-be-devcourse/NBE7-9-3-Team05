package com.back.motionit.domain.challenge.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
	@NotBlank(message = "제목은 필수입니다.")
	@Size(min = 2, max = 30, message = "제목은 30자 이내")
	String title,

	@NotBlank
	@Size(min = 2, max = 100, message = "설명은 100자 이내")
	String description,

	@NotNull
	Integer capacity,

	@NotNull
	Integer duration,

	@NotBlank
	String videoUrl,

	@NotBlank
	String imageFileName,

	@NotBlank
	String contentType
) {
}
