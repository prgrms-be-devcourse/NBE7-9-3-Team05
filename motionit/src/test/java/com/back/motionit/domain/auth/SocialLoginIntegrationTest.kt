package com.back.motionit.security.oauth;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.motionit.domain.auth.social.service.SocialAuthService;
import com.back.motionit.domain.user.entity.LoginType;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.security.jwt.JwtTokenDto;
import com.back.motionit.security.jwt.JwtTokenProvider;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("소셜로그인 통합 테스트")
class SocialLoginIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SocialAuthService socialAuthService;

	@Autowired
	private CustomOAuth2UserService customOAuth2UserService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	@DisplayName("소셜 로그인 통합 테스트 - CustomOAuth2UserService 호출 → DB 저장 → 토큰 발급 → /api/v1/users/profile 접근 성공")
	void completeE2E_DirectServiceCall() throws Exception {
		// STEP 1: OAuth2User 생성
		Long kakaoId = 123456789L;
		String nickname = "테스트유저";
		String profileImage = "https://k.kakaocdn.net/profile.jpg";

		OAuth2User oauth2User = createOAuth2User(kakaoId, nickname, profileImage);

		Map<String, Object> attributes = oauth2User.getAttributes();
		Long extractedKakaoId = Long.parseLong(attributes.get("id").toString());
		Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

		// STEP 2: 회원 가입 or 수정
		User user = socialAuthService.modifyOrJoin(
			extractedKakaoId,
			null,
			(String)properties.get("nickname"),
			"",
			LoginType.KAKAO,
			(String)properties.get("profile_image")
		);

		// STEP 3: 저장 검증
		entityManager.flush();
		entityManager.clear();

		User savedUser = userRepository.findByKakaoId(kakaoId).orElseThrow();

		assertThat(savedUser.getNickname()).isEqualTo(nickname);
		assertThat(savedUser.getUserProfile()).isEqualTo(profileImage);

		// STEP 4: JWT 발급
		JwtTokenDto tokens = socialAuthService.generateTokensById(savedUser.getId());
		assertThat(tokens.getAccessToken()).isNotBlank();
		assertThat(tokens.getRefreshToken()).isNotBlank();

		// STEP 5: Payload 검증
		var payload = jwtTokenProvider.payloadOrNull(tokens.getAccessToken());
		assertThat(payload.get("id")).isEqualTo(savedUser.getId());
		assertThat(payload.get("nickname")).isEqualTo(nickname);

		// STEP 6: 실제 보호 리소스 접근 (쿠키)
		mockMvc.perform(get("/api/v1/users/profile")
				.cookie(new Cookie("accessToken", tokens.getAccessToken())))
			.andDo(print())
			.andExpect(status().isOk());

		// STEP 7: 헤더 인증도 가능
		mockMvc.perform(get("/api/v1/users/profile")
				.header("Authorization", "Bearer " + tokens.getAccessToken()))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("신규 사용자 가입 → 토큰 발급 → 인증 성공")
	void newUserFlow_Complete() {
		Long kakaoId = 111222333L;
		String nickname = "신규유저";
		String profile = "profile.jpg";

		User user = socialAuthService.modifyOrJoin(
			kakaoId, null, nickname, "", LoginType.KAKAO, profile
		);

		assertThat(user.getId()).isNotNull();

		JwtTokenDto tokens = socialAuthService.generateTokensById(user.getId());
		assertThat(tokens.getAccessToken()).isNotBlank();

		var payload = socialAuthService.payloadOrNull(tokens.getAccessToken());
		assertThat(payload.get("nickname")).isEqualTo(nickname);
	}

	@Test
	@DisplayName("기존 사용자 재로그인 시 update() 동작")
	void existingUserRelogin_UpdatesInfo() {
		Long kakaoId = 444555666L;

		User old = socialAuthService.modifyOrJoin(
			kakaoId, null, "기존닉", "", LoginType.KAKAO, "old.png"
		);

		entityManager.flush();
		entityManager.clear();

		User updated = socialAuthService.modifyOrJoin(
			kakaoId, null, "변경닉", "", LoginType.KAKAO, "new.png"
		);

		assertThat(updated.getId()).isEqualTo(old.getId());
		assertThat(updated.getNickname()).isEqualTo("변경닉");
		assertThat(updated.getUserProfile()).isEqualTo("new.png");
	}

	@Test
	@DisplayName("로그인 → 보호 API 접근 → 로그아웃 → refreshToken 제거")
	void loginAccessLogout_Complete() throws Exception {
		User user = socialAuthService.modifyOrJoin(
			999888L, null, "로그아웃유저", "", LoginType.KAKAO, "profile.jpg"
		);

		JwtTokenDto tokens = socialAuthService.generateTokensById(user.getId());

		// 접근 성공
		mockMvc.perform(get("/api/v1/users/profile")
				.cookie(new Cookie("accessToken", tokens.getAccessToken())))
			.andExpect(status().isOk());

		// 로그아웃 처리 → refreshToken 제거
		User find = userRepository.findById(user.getId()).orElseThrow();
		find.removeRefreshToken();
		entityManager.flush();

		User loggedOut = userRepository.findById(user.getId()).orElseThrow();
		assertThat(loggedOut.getRefreshToken()).isNull();

		// AccessToken은 여전히 유효 → 접근 OK
		mockMvc.perform(get("/api/v1/users/profile")
				.cookie(new Cookie("accessToken", tokens.getAccessToken())))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("쿠키 기반 인증 성공")
	void customAuthenticationFilter_CookieBased() throws Exception {
		User user = socialAuthService.modifyOrJoin(
			777777L, null, "필터쿠키", "", LoginType.KAKAO, "p.jpg"
		);
		JwtTokenDto tokens = socialAuthService.generateTokensById(user.getId());

		mockMvc.perform(get("/api/v1/users/profile")
				.cookie(new Cookie("accessToken", tokens.getAccessToken())))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Authorization 헤더 인증 성공")
	void customAuthenticationFilter_HeaderBased() throws Exception {
		User user = socialAuthService.modifyOrJoin(
			888888L, null, "필터헤더", "", LoginType.KAKAO, "p.jpg"
		);
		JwtTokenDto tokens = socialAuthService.generateTokensById(user.getId());

		mockMvc.perform(get("/api/v1/users/profile")
				.header("Authorization", "Bearer " + tokens.getAccessToken()))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("토큰 없이 접근 → 401")
	void customAuthenticationFilter_NoToken_Returns401() throws Exception {
		mockMvc.perform(get("/api/v1/users/profile"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("잘못된 토큰 → 401")
	void customAuthenticationFilter_InvalidToken_Returns401() throws Exception {
		mockMvc.perform(get("/api/v1/users/profile")
				.header("Authorization", "Bearer invalid.token"))
			.andExpect(status().isUnauthorized());
	}


	// Helper Method
	private OAuth2User createOAuth2User(Long kakaoId, String nickname, String profileImage) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("id", kakaoId);

		Map<String, Object> properties = new HashMap<>();
		properties.put("nickname", nickname);
		properties.put("profile_image", profileImage);
		attributes.put("properties", properties);

		attributes.put("kakao_account", new HashMap<>());

		return new DefaultOAuth2User(
			List.of(new SimpleGrantedAuthority("ROLE_USER")),
			attributes,
			"id"
		);
	}
}
