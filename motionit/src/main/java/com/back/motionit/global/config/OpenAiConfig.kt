package com.back.motionit.global.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.theokanning.openai.service.OpenAiService;

@Configuration
@ConditionalOnProperty(name = "app.openai.enabled", havingValue = "true", matchIfMissing = true)
public class OpenAiConfig {

	@Value("${openai.api.key}")
	private String openaiApiKey;

	@Bean
	public OpenAiService openAiService() {
		return new OpenAiService(openaiApiKey, Duration.ofSeconds(60));
	}
}
