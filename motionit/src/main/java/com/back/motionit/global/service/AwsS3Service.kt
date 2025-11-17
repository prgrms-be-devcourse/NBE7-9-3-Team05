package com.back.motionit.global.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.time.LocalDate
import java.util.*

@Service
@ConditionalOnBean(S3Client::class)
@ConditionalOnProperty(name = ["app.aws.enabled"], havingValue = "true", matchIfMissing = false)
class AwsS3Service(
    private val s3: S3Client,
    private val preSigner: S3Presigner,

    @Value("\${aws.s3.bucket-name}")
    private val bucket: String,

    @Value("\${aws.s3.key-prefix}")
    private val keyPrefix: String,

    @Value("\${aws.s3.presign-minutes:10}")
    private val preSignMinutes: Long = 0,
) {
    fun buildObjectKey(originalFileName: String): String {
        val dot = originalFileName.lastIndexOf('.')
        val ext: String = if (dot > -1) {
            originalFileName.substring(dot)
        } else {
            ""
        }

        val datePath = LocalDate.now().toString().replace("-", "/")
        val uuid = UUID.randomUUID().toString()
        val prefix = if (keyPrefix.isBlank()) "" else "$keyPrefix/"
        return String.format("%s%s/%s%s", prefix, datePath, uuid, ext)
    }

    fun createUploadUrl(objectKey: String, contentType: String): String {
        val put = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentType)
            .build()

        val pre = preSigner.presignPutObject { builder: PutObjectPresignRequest.Builder ->
            builder
                .signatureDuration(Duration.ofMinutes(preSignMinutes))
                .putObjectRequest(put)
        }

        return pre.url().toString()
    }

    fun delete(objectKey: String) {
        s3.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build()
        )
    }
}
