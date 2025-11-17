package com.back.motionit.global.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("MotionIt API")
                .description("운동 그룹 챌린지 플랫폼 API 명세서")
                .version("v1.0.0")
        )

    @Bean
    fun groupApiV1(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("apiV1")
            .pathsToMatch("/api/v1/**")
            .build()

    @Bean
    fun groupHome(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("home")
            .pathsToExclude("/api/**")
            .build()
}
