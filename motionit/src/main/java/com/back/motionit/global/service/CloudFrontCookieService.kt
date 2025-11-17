package com.back.motionit.global.service;

import java.io.File;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import com.amazonaws.services.cloudfront.CloudFrontCookieSigner;
import com.amazonaws.services.cloudfront.util.SignerUtils;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnProperty(name = "app.aws.enabled", havingValue = "true", matchIfMissing = true)
@Profile("!test")
public class CloudFrontCookieService {
	@Value("${aws.cloudfront.domain}")
	private String cloudFrontDomain;

	@Value("${aws.cloudfront.key-id}")
	private String keyPairId;

	@Value("${aws.cloudfront.private-key-path}")
	private String privateKeyPath;

	private PrivateKey privateKey;

	@PostConstruct
	void init() throws Exception {
		File keyFile = ResourceUtils.getFile(privateKeyPath);
		this.privateKey = SignerUtils.loadPrivateKey(keyFile);
	}

	public void setSignedCookies(HttpServletResponse response, Duration ttl) {
		Date expiresOn = Date.from(Instant.now().plus(ttl));
		String resourcePattern = "https://" + cloudFrontDomain + "/*";

		CloudFrontCookieSigner.CookiesForCannedPolicy cookies =
			CloudFrontCookieSigner.getCookiesForCannedPolicy(
				SignerUtils.Protocol.https,
				cloudFrontDomain,
				privateKey,
				resourcePattern,
				keyPairId,
				expiresOn
			);

		add(response, cookies.getKeyPairId(), true);
		add(response, cookies.getSignature(), true);
		add(response, cookies.getExpires(), true);
	}

	private void add(HttpServletResponse response, Map.Entry<String, String> entry, boolean httpOnly) {
		Cookie cookie = new Cookie(entry.getKey(), entry.getValue());
		cookie.setPath("/");
		cookie.setSecure(true);
		cookie.setHttpOnly(httpOnly);
		response.addCookie(cookie);
	}
}
