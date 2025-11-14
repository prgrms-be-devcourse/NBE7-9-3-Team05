package com.back.motionit.domain.storage.dto;

public record UploadUrlResponse(
	String objectKey,
	String uploadUrl
) {
}
