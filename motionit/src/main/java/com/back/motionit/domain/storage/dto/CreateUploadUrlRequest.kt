package com.back.motionit.domain.storage.dto

import jakarta.validation.constraints.NotBlank

data class CreateUploadUrlRequest(
	@field:NotBlank
	val originalFileName: String,

	@field:NotBlank
	val contentType: String,

	val objectKey: String
)
