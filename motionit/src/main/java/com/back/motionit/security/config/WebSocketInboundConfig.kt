package com.back.motionit.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.back.motionit.security.socket.StompAuthChannelInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebSocketInboundConfig implements WebSocketMessageBrokerConfigurer {
	private final StompAuthChannelInterceptor authInterceptor;

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(authInterceptor);
	}
}
