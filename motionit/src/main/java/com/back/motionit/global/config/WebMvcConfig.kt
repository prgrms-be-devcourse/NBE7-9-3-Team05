package com.back.motionit.global.config

import com.back.motionit.global.config.aws.CloudFrontCookieInterceptor
import com.back.motionit.global.service.CloudFrontCookieService
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@ConditionalOnBean(CloudFrontCookieService::class)
class WebMvcConfig(
    private val cloudFrontCookieInterceptor: CloudFrontCookieInterceptor
) : WebMvcConfigurer {


    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .exposedHeaders("*")
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(cloudFrontCookieInterceptor)
            .addPathPatterns("/api/v1/challenge/rooms", "/api/v1/challenge/rooms/**")
    }
}
