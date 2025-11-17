package com.back.motionit.domain.auth.local.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.back.motionit.domain.auth.dto.SignupRequest;
import com.back.motionit.domain.auth.local.service.LocalAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("LocalAuthController @Valid 검증 실패 테스트")
public class LocalAuthControllerValidationTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		LocalAuthService mockService = Mockito.mock(LocalAuthService.class);
		LocalAuthController controller = new LocalAuthController(mockService);

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		this.mockMvc = MockMvcBuilders
			.standaloneSetup(controller)
			.setValidator(validator)
			.build();

		this.objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("이메일이 비어있으면 400 반환")
	void signup_email_blank() throws Exception {
		SignupRequest dto = new SignupRequest("", "password123", "닉네임");
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("이메일이 공백이면 400 반환")
	void signup_email_whitespace() throws Exception {
		SignupRequest dto = new SignupRequest("   ", "password123", "닉네임");
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("이메일 형식이 아니면 400 반환")
	void signup_email_invalid_format() throws Exception {
		SignupRequest dto = new SignupRequest("not-email", "password123", "닉네임");
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("이메일이 100자 초과면 400 반환")
	void signup_email_too_long() throws Exception {
		String longEmail = "a".repeat(101) + "@test.com";
		SignupRequest dto = new SignupRequest(longEmail, "password123", "닉네임");
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("비밀번호가 비어있으면 400 반환")
	void signup_password_blank() throws Exception {
		SignupRequest dto = new SignupRequest("test@email.com", "", "닉네임");
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("비밀번호가 공백이면 400 반환")
	void signup_password_whitespace() throws Exception {
		SignupRequest dto = new SignupRequest("test@email.com", "   ", "닉네임");
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("비밀번호가 8자 미만이면 400 반환")
	void signup_password_too_short() throws Exception {
		SignupRequest dto = new SignupRequest("test@email.com", "short", "닉네임");
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("비밀번호가 30자 초과면 400 반환")
	void signup_password_too_long() throws Exception {
		String longPassword = "a".repeat(31);
		SignupRequest dto = new SignupRequest("test@email.com", longPassword, "닉네임");
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("닉네임이 비어있으면 400 반환")
	void signup_nickname_blank() throws Exception {
		SignupRequest dto = new SignupRequest("test@email.com", "password123", "");
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("닉네임이 공백이면 400 반환")
	void signup_nickname_whitespace() throws Exception {
		SignupRequest dto = new SignupRequest("test@email.com", "password123", "   ");
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("닉네임이 3자 미만이면 400 반환")
	void signup_nickname_too_short() throws Exception {
		SignupRequest dto = new SignupRequest("test@email.com", "password123", "ab");
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("닉네임이 10자 초과면 400 반환")
	void signup_nickname_too_long() throws Exception {
		String longNickname = "a".repeat(11);
		SignupRequest dto = new SignupRequest("test@email.com", "password123", longNickname);
		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
			.andExpect(status().isBadRequest());
	}
}
