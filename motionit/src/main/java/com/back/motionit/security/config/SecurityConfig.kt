package com.back.motionit.security.config

import com.back.motionit.security.CustomAuthenticationFilter
import com.back.motionit.security.handler.CustomOAuth2LoginSuccessHandler
import com.back.motionit.security.oauth.CustomOAuth2AuthorizationRequestResolver
import com.back.motionit.security.oauth.CustomOAuth2UserService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.SecurityContextHolderFilter
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = ["app.security.enabled"], havingValue = "true", matchIfMissing = true)
class SecurityConfig {

    @Bean
    @Throws(Exception::class)
    fun filterChain(
        http: HttpSecurity,
        customAuthenticationFilter: ObjectProvider<CustomAuthenticationFilter>,
        clientRepoProvider: ObjectProvider<ClientRegistrationRepository>,
        customOAuth2LoginSuccessHandler: ObjectProvider<CustomOAuth2LoginSuccessHandler>,
        customOAuth2AuthorizationRequestResolver: ObjectProvider<CustomOAuth2AuthorizationRequestResolver>,
        customOAuth2UserService: ObjectProvider<CustomOAuth2UserService>,
        @Value("\${app.oauth2.enabled:true}") oauth2Enabled: Boolean
    ): SecurityFilterChain {

        http.run {
            cors(Customizer.withDefaults())

            authorizeHttpRequests { reg ->
                reg.requestMatchers(
                    "/favicon.ico",
                    "/h2-console/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/.well-known/**",
                    "/api/v1/storage/**",
                    "/api/v1/auth/**",
                    "/ws/**",
                    "/actuator/**"
                ).permitAll()
                    .anyRequest().authenticated()
            }

            csrf { it.disable() }

            headers {
                it.addHeaderWriter(
                    XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)
                )
            }

            sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        }

        // CustomAuthenticationFilter 추가
        customAuthenticationFilter.ifAvailable?.let { filter ->
            http.addFilterAfter(filter, SecurityContextHolderFilter::class.java)
        }

        // OAuth2 설정 (조건부 활성화)
        val clientRepo = clientRepoProvider.ifAvailable
        if (oauth2Enabled && clientRepo != null) {
            http.oauth2Login { oauth2 ->
                customOAuth2AuthorizationRequestResolver.ifAvailable?.let { resolver ->
                    oauth2.authorizationEndpoint { it.authorizationRequestResolver(resolver) }
                }

                customOAuth2UserService.ifAvailable?.let { userService ->
                    oauth2.userInfoEndpoint { it.userService(userService) }
                }

                customOAuth2LoginSuccessHandler.ifAvailable?.let { handler ->
                    oauth2.successHandler(handler)
                }
            }
        } else {
            http.oauth2Login { it.disable() }
        }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("http://localhost:3000")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            exposedHeaders = listOf("*")
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun messageAuthorizationManager(): AuthorizationManager<Message<*>> {
        return MessageMatcherDelegatingAuthorizationManager.builder().apply {
            simpSubscribeDestMatchers("/topic/challenge/rooms").permitAll()
            simpSubscribeDestMatchers("/topic/challenge/rooms/*").authenticated()
            simpDestMatchers("/app/**").authenticated()
            anyMessage().denyAll()
        }.build()
    }
}
