package com.back.motionit.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@SpringBootTest(properties = {
	// 스프링 시큐리티/리소스서버/OAuth2 클라 자동설정 전부 제외
	"spring.autoconfigure.exclude="
		+ "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration,"
		+
		"org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
	"app.security.enabled=false",
	"app.oauth2.enabled=false",
	"app.aws.enabled=false",
	"app.openai.enabled=false",
	"debug=true",
	"logging.level.org.springframework.boot.autoconfigure.condition=INFO"
})
@AutoConfigureMockMvc(addFilters = false)
@Tag("integration")
@Transactional
public @interface IntegrationTest {
}
