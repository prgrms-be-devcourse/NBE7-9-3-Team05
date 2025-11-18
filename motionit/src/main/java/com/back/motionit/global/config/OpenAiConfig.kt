package com.back.motionit.global.config

import com.theokanning.openai.service.OpenAiService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConditionalOnProperty(name = ["app.openai.enabled"], havingValue = "true", matchIfMissing = true)
class OpenAiConfig(
    @Value("\${openai.api.key:}")
    private val openaiApiKey: String
) {

    @Bean
    fun openAiService(): OpenAiService =
        OpenAiService(openaiApiKey, Duration.ofSeconds(60))
}
