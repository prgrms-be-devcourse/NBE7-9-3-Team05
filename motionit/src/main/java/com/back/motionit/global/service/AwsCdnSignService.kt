package com.back.motionit.global.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.back.motionit.global.config.aws.CloudFrontSignConfig;

import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnProperty(name = "app.aws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AwsCdnSignService {

	private final CloudFrontSignConfig.CloudFrontSigner signer;

	@Value("${aws.cloudfront.signed-url-minutes:10}")
	private long signedMinutes;

	public String sign(String objectKey) {
		Instant expires = Instant.now().plus(signedMinutes, ChronoUnit.MINUTES);
		return signer.signUrl(objectKey, expires);
	}
}
