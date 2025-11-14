package com.back.motionit.domain.challenge.room.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateRoomRequest(
	@field:NotBlank(message = "제목은 필수입니다.")
	@field:Size(min = 2, max = 30, message = "제목은 30자 이내")
	val title: String,

	@field:NotBlank
	@field:Size(min = 2, max = 100, message = "설명은 100자 이내")
	val description: String,

	@field:NotNull
	val capacity: Int,

	@field:NotNull
	val duration: Int,

	@field:NotBlank
	val videoUrl: String,

	@field:NotBlank
	val imageFileName: String,

	@field:NotBlank
	val contentType: String
)
