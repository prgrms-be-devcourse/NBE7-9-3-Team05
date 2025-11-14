package com.back.motionit.domain.storage.controller

import com.back.motionit.domain.storage.api.StorageApi
import com.back.motionit.domain.storage.api.response.StorageHttp
import com.back.motionit.domain.storage.dto.CdnUrlResponse
import com.back.motionit.domain.storage.dto.CreateUploadUrlRequest
import com.back.motionit.domain.storage.dto.UploadUrlResponse
import com.back.motionit.global.error.code.CommonErrorCode
import com.back.motionit.global.respoonsedata.ResponseData
import com.back.motionit.global.respoonsedata.ResponseData.Companion.error
import com.back.motionit.global.respoonsedata.ResponseData.Companion.success
import com.back.motionit.global.service.AwsCdnSignService
import com.back.motionit.global.service.AwsS3Service
import jakarta.validation.Valid
import org.springframework.beans.factory.ObjectProvider
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/storage")
class StorageController(
    private val s3Provider: ObjectProvider<AwsS3Service>,
    private val cdnProvider: ObjectProvider<AwsCdnSignService>,
) : StorageApi {
    override fun createUploadUrl(
        @RequestBody @Valid request: CreateUploadUrlRequest
    ): ResponseData<UploadUrlResponse> {
        val s3Service = s3Provider.getIfAvailable()
            ?: return error(CommonErrorCode.BAD_REQUEST)

        val objectKey = request.objectKey
            .takeIf { it.isNotBlank() }
            ?: s3Service.buildObjectKey(request.originalFileName)

        val url = s3Service.createUploadUrl(
            objectKey,
            request.contentType
        )

        return success(
            StorageHttp.CREATE_AWS_URL_SUCCESS_CODE,
            StorageHttp.CREATE_AWS_URL_SUCCESS_MESSAGE,
            UploadUrlResponse(objectKey, url)
        )
    }

    override fun signCdnUrl(@RequestParam("key") key: String): ResponseData<CdnUrlResponse> {
        val cdn = cdnProvider.getIfAvailable()
            ?: return error(CommonErrorCode.BAD_REQUEST)

        val url = cdn.sign(key)
        return success(
            StorageHttp.CREATE_CDN_URL_SUCCESS_CODE,
            StorageHttp.CREATE_CDN_URL_SUCCESS_MESSAGE,
            CdnUrlResponse(key, url)
        )
    }
}
