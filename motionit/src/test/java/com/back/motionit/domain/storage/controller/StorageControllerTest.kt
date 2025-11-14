package com.back.motionit.domain.storage.controller

import com.back.motionit.domain.storage.api.response.StorageHttp
import com.back.motionit.domain.storage.dto.CreateUploadUrlRequest
import com.back.motionit.global.service.AwsCdnSignService
import com.back.motionit.global.service.AwsS3Service
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@ExtendWith(MockitoExtension::class)
class StorageControllerTest {
    lateinit var mvc: MockMvc

    var mapper: ObjectMapper = ObjectMapper()

    @Mock
    lateinit var s3Provider: ObjectProvider<AwsS3Service>

    @Mock
    lateinit var cdnProvider: ObjectProvider<AwsCdnSignService>

    @Mock
    lateinit var s3Service: AwsS3Service

    @Mock
    lateinit var cdnSignService: AwsCdnSignService
    @BeforeEach
    fun setUp() {
        val controller = StorageController(s3Provider, cdnProvider)
        mvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Nested
    @DisplayName("POST " + UPLOAD_URL)
    internal inner class S3Url {
        @Test
        @DisplayName("성공 - objectKey 미지정 → buildObjectKey 호출 후 presigned URL 반환")
        fun createUploadUrl_generateKey() {
            BDDMockito.given(s3Provider.getIfAvailable()).willReturn(s3Service)
            val generatedKey = "uploads/2025/10/17/uuid.png"
            val contentType = "image/png"
            val presigned = "https://s3-presigned-url.example"

            BDDMockito.given(s3Service.buildObjectKey("cat.png")).willReturn(generatedKey)
            BDDMockito.given(s3Service.createUploadUrl(generatedKey, contentType)).willReturn(presigned)

            val request = CreateUploadUrlRequest("cat.png", contentType, "")

            mvc.perform(
                MockMvcRequestBuilders.post(UPLOAD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.resultCode").value(StorageHttp.CREATE_AWS_URL_SUCCESS_CODE)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(StorageHttp.CREATE_AWS_URL_SUCCESS_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.objectKey").value(generatedKey))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.uploadUrl").value(presigned))
        }

        @Test
        @DisplayName("성공 - objectKey 지정 → 그대로 사용하여 presigned URL 반환")
        fun createUploadUrl_withGivenKey() {
            BDDMockito.given(s3Provider.getIfAvailable()).willReturn(s3Service)
            val givenKey = "uploads/2025/10/17/my.png"
            val contentType = "image/png"
            val presigned = "https://s3-presigned-url-2.example"

            BDDMockito.given(s3Service.createUploadUrl(givenKey, contentType)).willReturn(presigned)

            val req = CreateUploadUrlRequest("cat.png", contentType, givenKey)

            mvc.perform(
                MockMvcRequestBuilders.post(UPLOAD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.objectKey").value(givenKey))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.uploadUrl").value(presigned))
        }

        @Test
        @DisplayName("실패 - contentType 누락 시 400 (DTO에 @NotBlankk가 붙어있다는 가정)")
        fun badRequest_whenContentTypeMissing() {
            val request = CreateUploadUrlRequest("cat.png", "", "")

            mvc.perform(
                MockMvcRequestBuilders.post(UPLOAD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
        }
    }

    @Nested
    @DisplayName("GET " + CDN_URL)
    internal inner class CdnUrl {
        @Test
        @DisplayName("성공 - Key 파라미터로 서명된 CloudFront URL 반환")
        fun signCdnUrl_ok() {
            // given
            BDDMockito.given(cdnProvider.getIfAvailable()).willReturn(cdnSignService)
            val key = "uploads/2025/10/17/cat.png"
            val signedUrl =
                "https://dxxx.cloudfront.net/uploads/2025/10/17/cat.png?Expires=1734460000&Key-Pair-Id=APKAXXX&Signature=abc"
            BDDMockito.given(cdnSignService.sign(key)).willReturn(signedUrl)

            // when/then
            mvc.perform(MockMvcRequestBuilders.get(CDN_URL).param("key", key))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.resultCode").value(StorageHttp.CREATE_CDN_URL_SUCCESS_CODE)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(StorageHttp.CREATE_CDN_URL_SUCCESS_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.objectKey").value(key))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.cdnUrl").value(signedUrl))
        }

        @Test
        @DisplayName("실패 - key 파라미터 누락 시 400")
        fun signCdnUrl_missingKey_badRequest() {
            mvc.perform(MockMvcRequestBuilders.get(CDN_URL)) // key 미전달
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
        }
    }

    companion object {
        private const val UPLOAD_URL = "/api/v1/storage/upload-url"
        private const val CDN_URL = "/api/v1/storage/cdn-url"
    }
}
