package com.back.motionit.security.oauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.motionit.domain.auth.social.service.SocialAuthService;
import com.back.motionit.domain.user.entity.LoginType;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.security.SecurityUser;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

	@Mock
	private SocialAuthService socialAuthService;

	@Test
	@DisplayName("카카오 attributes 파싱 → modifyOrJoin 호출 → SecurityUser 생성")
	void loadUser_InternalLogic_Success() {

		// given
		Long kakaoId = 12345L;
		String nickname = "testUser";
		String profileImage = "https://image.jpg";

		Map<String, Object> attributes = Map.of(
			"id", kakaoId,
			"properties", Map.of(
				"nickname", nickname,
				"profile_image", profileImage
			),
			"kakao_account", Map.of()
		);

		OAuth2User fakeOAuthUser = new DefaultOAuth2User(
			List.of(new SimpleGrantedAuthority("ROLE_USER")),
			attributes,
			"id"
		);

		// modifyOrJoin 결과 stub
		User user = User.builder()
			.kakaoId(kakaoId)
			.nickname(nickname)
			.userProfile(profileImage)
			.loginType(LoginType.KAKAO)
			.build();
		ReflectionTestUtils.setField(user, "id", 1L);

		given(socialAuthService.modifyOrJoin(kakaoId, null, nickname, "", LoginType.KAKAO, profileImage))
			.willReturn(user);


		// when
		OAuth2User oAuth2User = fakeOAuthUser;

		Long parsedId = Long.parseLong(oAuth2User.getName());
		Map<String, Object> props = (Map<String, Object>) oAuth2User.getAttributes().get("properties");

		String parsedNickname = (String) props.get("nickname");
		String parsedProfile = (String) props.get("profile_image");

		User resultUser = socialAuthService.modifyOrJoin(
			parsedId, null, parsedNickname, "", LoginType.KAKAO, parsedProfile
		);

		String pw = (resultUser.getPassword() != null && !resultUser.getPassword().isEmpty())
			? resultUser.getPassword()
			: "OAUTH2_USER";

		SecurityUser securityUser = new SecurityUser(
			resultUser.getId(),
			pw,
			resultUser.getNickname(),
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);


		// then
		assertThat(securityUser.getId()).isEqualTo(1L);
		assertThat(securityUser.getNickname()).isEqualTo("testUser");
		assertThat(securityUser.getPassword()).isEqualTo("OAUTH2_USER");
		assertThat(securityUser.getAuthorities()).extracting("authority")
			.containsExactly("ROLE_USER");

		verify(socialAuthService).modifyOrJoin(
			kakaoId, null, nickname, "", LoginType.KAKAO, profileImage
		);
	}
}
