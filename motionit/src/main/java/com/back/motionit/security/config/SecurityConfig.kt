package com.back.motionit.security.config;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.back.motionit.security.CustomAuthenticationFilter;
import com.back.motionit.security.handler.CustomOAuth2LoginSuccessHandler;
import com.back.motionit.security.oauth.CustomOAuth2AuthorizationRequestResolver;
import com.back.motionit.security.oauth.CustomOAuth2UserService;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(
		HttpSecurity http,
		ObjectProvider<CustomAuthenticationFilter> customAuthenticationFilter,
		ObjectProvider<ClientRegistrationRepository> clientRepoProvider,
		ObjectProvider<CustomOAuth2LoginSuccessHandler> customOAuth2LoginSuccessHandler,
		ObjectProvider<CustomOAuth2AuthorizationRequestResolver> customOAuth2AuthorizationRequestResolver,
		ObjectProvider<CustomOAuth2UserService> customOAuth2UserService,
		@Value("${app.oauth2.enabled:true}") boolean oauth2Enabled
	) throws Exception {

		http
			.cors(Customizer.withDefaults())
			.authorizeHttpRequests(reg -> reg
				.requestMatchers("/favicon.ico").permitAll()
				.requestMatchers("/h2-console/**").permitAll()
				.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.requestMatchers("/.well-known/**").permitAll()
				.requestMatchers("/api/v1/storage/**").permitAll()
				.requestMatchers("/api/v1/auth/**").permitAll()
				.requestMatchers("/ws/**").permitAll()
				.requestMatchers("/actuator/**").permitAll()    // 모니터링/Actuator 관련
				.anyRequest().authenticated()
			)
			.csrf(AbstractHttpConfigurer::disable)
			.headers(headers -> headers.addHeaderWriter(
				new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)
			))
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		// CustomAuthenticationFilter가 있으면 연결
		CustomAuthenticationFilter authFilter = customAuthenticationFilter.getIfAvailable();
		if (authFilter != null) {
			http.addFilterAfter(authFilter, SecurityContextHolderFilter.class);
		}

		// OAuth2는 enabled + ClientRegistrationRepository가 있을 때만 구성
		var clientRepo = clientRepoProvider.getIfAvailable();
		if (oauth2Enabled && clientRepo != null) {
			http.oauth2Login(oauth2 -> {
				var resolver = customOAuth2AuthorizationRequestResolver.getIfAvailable();
				var userService = customOAuth2UserService.getIfAvailable();
				var successHandler = customOAuth2LoginSuccessHandler.getIfAvailable();

				if (resolver != null) {
					oauth2.authorizationEndpoint(ae -> ae.authorizationRequestResolver(resolver));
				}

				if (userService != null) {
					oauth2.userInfoEndpoint(ui -> ui.userService(userService));
				}

				if (successHandler != null) {
					oauth2.successHandler(successHandler);
				}
			});
		} else {
			http.oauth2Login(AbstractHttpConfigurer::disable);
		}

		return http.build();
	}

	@Bean
	public UrlBasedCorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(List.of("http://localhost:3000"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);
		configuration.setExposedHeaders(List.of("*"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public AuthorizationManager<Message<?>> messageAuthorizationManager() {
		MessageMatcherDelegatingAuthorizationManager.Builder builder =
			MessageMatcherDelegatingAuthorizationManager.builder();
		builder.simpSubscribeDestMatchers("/topic/challenge/rooms").permitAll();
		builder.simpSubscribeDestMatchers("/topic/challenge/rooms/*").authenticated();
		builder.simpDestMatchers("/app/**").authenticated();
		builder.anyMessage().denyAll();
		return builder.build();
	}
}
