package com.back.motionit.global.config.aws;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.services.cloudfront.util.SignerUtils;
import com.back.motionit.global.error.code.ConfigErrorCode;
import com.back.motionit.global.error.exception.BusinessException;

@Configuration
@ConditionalOnProperty(name = "app.aws.enabled", havingValue = "true", matchIfMissing = true)
public class CloudFrontSignConfig {

	@Value("${aws.cloudfront.domain}")
	private String cloudFrontDomain;

	@Value("${aws.cloudfront.key-id}")
	private String keyPairId;

	@Value("${aws.cloudfront.private-key-path}")
	private String privateKeyPath;

	@Bean
	public CloudFrontSigner cloudFrontSigner() {
		return new CloudFrontSigner(cloudFrontDomain, keyPairId, loadPrivateKey(privateKeyPath));
	}

	private PrivateKey loadPrivateKey(String location) {
		try {
			if (location.startsWith("classpath:")) {
				String cp = location.replace("classpath:", "");
				try (InputStream in = getClass().getResourceAsStream(cp)) {
					if (in == null) {
						throw new IllegalStateException("Classpath resource not found: " + cp);
					}
					Path tmp = Files.createTempFile("cf-key-", ".pem");
					Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
					try {
						return SignerUtils.loadPrivateKey(tmp.toFile());
					} finally {
						Files.deleteIfExists(tmp);
					}
				}
			} else {
				String path = location.replaceFirst("^file:", "");
				return SignerUtils.loadPrivateKey(new File(path));
			}
		} catch (Exception e) {
			throw new BusinessException(ConfigErrorCode.FAILED_LOAD_PRIVATE_KEY);
		}
	}

	public static class CloudFrontSigner {
		private final String domain;
		private final String keyPairId;
		private final PrivateKey privateKey;

		public CloudFrontSigner(String domain, String keyPairId, PrivateKey privateKey) {
			this.domain = domain;
			this.keyPairId = keyPairId;
			this.privateKey = privateKey;
		}

		public String signUrl(String objectKey, Instant expiresAt) {
			String resourceUrl = String.format("https://%s/%s", domain, objectKey);
			Date expires = Date.from(expiresAt);
			try {
				return CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
					resourceUrl, keyPairId, privateKey, expires
				);
			} catch (Exception e) {
				throw new BusinessException(ConfigErrorCode.FAILED_SIGN_CLOUD_FRONT_URL);
			}
		}
	}
}
