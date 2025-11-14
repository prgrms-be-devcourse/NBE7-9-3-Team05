package com.back.motionit.domain.storage.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUploadUrlRequest(
	@NotBlank String originalFileName,
	@NotBlank String contentType,
	String objectKey
) {
}
