package com.back.motionit.domain.storage.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.back.motionit.domain.storage.api.response.StorageHttp;
import com.back.motionit.domain.storage.dto.CreateUploadUrlRequest;
import com.back.motionit.global.service.AwsCdnSignService;
import com.back.motionit.global.service.AwsS3Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class StorageControllerTest {

	MockMvc mvc;

	ObjectMapper mapper = new ObjectMapper();

	@Mock
	ObjectProvider<AwsS3Service> s3Provider;

	@Mock
	ObjectProvider<AwsCdnSignService> cdnProvider;

	@Mock
	AwsS3Service s3Service;

	@Mock
	AwsCdnSignService cdnSignService;
	private static final String UPLOAD_URL = "/api/v1/storage/upload-url";
	private static final String CDN_URL = "/api/v1/storage/cdn-url";

	@BeforeEach
	void setUp() {
		StorageController controller = new StorageController(s3Provider, cdnProvider);
		mvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Nested
	@DisplayName("POST " + UPLOAD_URL)
	class S3Url {

		@Test
		@DisplayName("성공 - objectKey 미지정 → buildObjectKey 호출 후 presigned URL 반환")
		void createUploadUrl_generateKey() throws Exception {
			given(s3Provider.getIfAvailable()).willReturn(s3Service);
			String generatedKey = "uploads/2025/10/17/uuid.png";
			String contentType = "image/png";
			String presigned = "https://s3-presigned-url.example";

			given(s3Service.buildObjectKey("cat.png")).willReturn(generatedKey);
			given(s3Service.createUploadUrl(generatedKey, contentType)).willReturn(presigned);

			var request = new CreateUploadUrlRequest("cat.png", contentType, null);

			mvc.perform(post(UPLOAD_URL)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.resultCode").value(StorageHttp.CREATE_AWS_URL_SUCCESS_CODE))
				.andExpect(jsonPath("$.msg").value(StorageHttp.CREATE_AWS_URL_SUCCESS_MESSAGE))
				.andExpect(jsonPath("$.data.objectKey").value(generatedKey))
				.andExpect(jsonPath("$.data.uploadUrl").value(presigned));
		}

		@Test
		@DisplayName("성공 - objectKey 지정 → 그대로 사용하여 presigned URL 반환")
		void createUploadUrl_withGivenKey() throws Exception {
			given(s3Provider.getIfAvailable()).willReturn(s3Service);
			String givenKey = "uploads/2025/10/17/my.png";
			String contentType = "image/png";
			String presigned = "https://s3-presigned-url-2.example";

			given(s3Service.createUploadUrl(givenKey, contentType)).willReturn(presigned);

			var req = new CreateUploadUrlRequest("cat.png", contentType, givenKey);

			mvc.perform(post(UPLOAD_URL)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(req)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.objectKey").value(givenKey))
				.andExpect(jsonPath("$.data.uploadUrl").value(presigned));
		}

		@Test
		@DisplayName("실패 - contentType 누락 시 400 (DTO에 @NotBlankk가 붙어있다는 가정)")
		void badRequest_whenContentTypeMissing() throws Exception {
			var request = new CreateUploadUrlRequest("cat.png", null, null);

			mvc.perform(post(UPLOAD_URL)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("GET " + CDN_URL)
	class CdnUrl {

		@Test
		@DisplayName("성공 - Key 파라미터로 서명된 CloudFront URL 반환")
		void signCdnUrl_ok() throws Exception {
			// given
			given(cdnProvider.getIfAvailable()).willReturn(cdnSignService);
			String key = "uploads/2025/10/17/cat.png";
			String signedUrl = "https://dxxx.cloudfront.net/uploads/2025/10/17/cat.png?Expires=1734460000&Key-Pair-Id=APKAXXX&Signature=abc";
			given(cdnSignService.sign(key)).willReturn(signedUrl);

			// when/then
			mvc.perform(get(CDN_URL).param("key", key))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.resultCode").value(StorageHttp.CREATE_CDN_URL_SUCCESS_CODE))
				.andExpect(jsonPath("$.msg").value(StorageHttp.CREATE_CDN_URL_SUCCESS_MESSAGE))
				.andExpect(jsonPath("$.data.objectKey").value(key))
				.andExpect(jsonPath("$.data.cdnUrl").value(signedUrl));
		}

		@Test
		@DisplayName("실패 - key 파라미터 누락 시 400")
		void signCdnUrl_missingKey_badRequest() throws Exception {
			mvc.perform(get(CDN_URL)) // key 미전달
				.andExpect(status().isBadRequest());
		}
	}
}
