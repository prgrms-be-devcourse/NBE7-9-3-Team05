package com.back.motionit.global.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@ConditionalOnBean(S3Client.class)
@ConditionalOnProperty(name = "app.aws.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class AwsS3Service {

	private final S3Client s3;
	private final S3Presigner preSigner;

	@Value("${aws.s3.bucket-name}")
	private String bucket;

	@Value("${aws.s3.key-prefix}")
	private String keyPrefix;

	@Value("${aws.s3.presign-minutes:10}")
	private long preSignMinutes;

	public static String encodeFileName(String name) {
		return URLEncoder.encode(name, StandardCharsets.UTF_8)
			.replace("+", "%20");
	}

	public String buildObjectKey(String originalFileName) {
		String ext = "";
		int dot = originalFileName.lastIndexOf('.');
		if (dot > -1) {
			ext = originalFileName.substring(dot);
		}

		String datePath = java.time.LocalDate.now().toString().replace("-", "/");
		String uuid = UUID.randomUUID().toString();
		String prefix = (keyPrefix == null || keyPrefix.isBlank()) ? "" : keyPrefix + "/";
		return String.format("%s%s/%s%s", prefix, datePath, uuid, ext);
	}

	public String createUploadUrl(String objectKey, String contentType) {
		PutObjectRequest put = PutObjectRequest.builder()
			.bucket(bucket)
			.key(objectKey)
			.contentType(contentType)
			.build();

		PresignedPutObjectRequest pre = preSigner.presignPutObject(b -> b
			.signatureDuration(Duration.ofMinutes(preSignMinutes))
			.putObjectRequest(put)
		);

		return pre.url().toString();
	}

	public void delete(String objectKey) {
		s3.deleteObject(DeleteObjectRequest.builder()
			.bucket(bucket)
			.key(objectKey)
			.build()
		);
	}
}
