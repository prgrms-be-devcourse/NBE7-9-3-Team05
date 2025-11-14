package com.back.motionit.domain.storage.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.back.motionit.domain.storage.api.response.StorageHttp;
import com.back.motionit.domain.storage.dto.CdnUrlResponse;
import com.back.motionit.domain.storage.dto.CreateUploadUrlRequest;
import com.back.motionit.domain.storage.dto.UploadUrlResponse;
import com.back.motionit.global.respoonsedata.ResponseData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "AWS S3 저장소", description = "운동방, 사용자 프로필 이미지를 S3에 업로드하기 위한 API")
public interface StorageApi {
	@PostMapping("/upload-url")
	@Operation(summary = "AWS S3 업로드 url 생성", description = "이미지를 업로드 할 수 있는 Pre-signed URL을 생성합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = StorageHttp.CREATE_AWS_URL_SUCCESS_CODE, description =
			StorageHttp.CREATE_AWS_URL_SUCCESS_MESSAGE,
			content = @Content(schema = @Schema(implementation = UploadUrlResponse.class)))
	})
	ResponseData<UploadUrlResponse> createUploadUrl(@RequestBody @Valid CreateUploadUrlRequest request);

	@GetMapping("/cdn-url")
	@Operation(summary = "AWS cdn url 생성", description = "이미지를 확인 할 수 있는 URL을 생성합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = StorageHttp.CREATE_CDN_URL_SUCCESS_CODE, description =
			StorageHttp.CREATE_CDN_URL_SUCCESS_MESSAGE,
			content = @Content(schema = @Schema(implementation = CdnUrlResponse.class)))
	})
	ResponseData<CdnUrlResponse> signCdnUrl(@RequestParam("key") String key);
}
