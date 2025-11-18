package com.back.motionit.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.back.motionit.global.config.aws.CloudFrontCookieInterceptor;
import com.back.motionit.global.service.CloudFrontCookieService;

import lombok.RequiredArgsConstructor;

@Configuration
@ConditionalOnBean(CloudFrontCookieService.class)
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final CloudFrontCookieInterceptor cloudFrontCookieInterceptor;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOriginPatterns("http://localhost:3000")
			.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
			.allowedHeaders("*")
			.allowCredentials(true)
			.exposedHeaders("*");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(cloudFrontCookieInterceptor)
			.addPathPatterns("/api/v1/challenge/rooms", "/api/v1/challenge/rooms/**");
	}
}
