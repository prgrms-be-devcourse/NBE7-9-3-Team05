package com.back.motionit.domain.auth.social.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.motionit.domain.auth.service.AuthTokenService;
import com.back.motionit.domain.user.entity.LoginType;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.error.code.AuthErrorCode;
import com.back.motionit.global.error.exception.BusinessException;
import com.back.motionit.security.jwt.JwtTokenDto;
import com.back.motionit.security.jwt.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialAuthService 단위 테스트")
class SocialAuthServiceTest {

	@InjectMocks
	private SocialAuthService socialAuthService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthTokenService authTokenService;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("카카오 신규 회원가입 - 성공")
	void join_KakaoUser_Success() {
		// given
		Long kakaoId = 123456789L;
		String email = null;
		String nickname = "테스터";
		String password = "";
		LoginType loginType = LoginType.KAKAO;
		String userProfile = "https://profile.com/image.jpg";

		given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.empty());
		given(userRepository.findByNickname(nickname)).willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		User result = socialAuthService.modifyOrJoin(kakaoId, email, nickname, password, loginType, userProfile);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getKakaoId()).isEqualTo(kakaoId);
		assertThat(result.getEmail()).isNull();
		assertThat(result.getNickname()).isEqualTo(nickname);
		assertThat(result.getPassword()).isNull();
		assertThat(result.getLoginType()).isEqualTo(loginType);
		assertThat(result.getUserProfile()).isEqualTo(userProfile);

		verify(userRepository).findByNickname(nickname);
		verify(passwordEncoder, never()).encode(anyString());
		verify(userRepository).save(any(User.class));
	}

	@Test
	@DisplayName("카카오 회원가입 - 닉네임 중복으로 실패")
	void join_NicknameDuplicated_ThrowsException() {
		// given
		Long kakaoId = 123456789L;
		String email = null;
		String nickname = "중복닉네임";
		String password = "";
		LoginType loginType = LoginType.KAKAO;
		String userProfile = "https://profile.com/image.jpg";

		User existingUser = User.builder()
			.nickname(nickname)
			.build();
		given(userRepository.findByNickname(nickname)).willReturn(Optional.of(existingUser));

		// when & then
		assertThatThrownBy(() ->
			socialAuthService.modifyOrJoin(kakaoId, email, nickname, password, loginType, userProfile))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.NICKNAME_DUPLICATED);

		verify(userRepository).findByNickname(nickname);
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	@DisplayName("기존 카카오 회원 정보 수정")
	void modifyOrJoin_ExistingKakaoUser_UpdatesUser() {
		// given
		Long kakaoId = 123456789L;
		String email = null;
		String newNickname = "새닉네임";
		String password = "";
		LoginType loginType = LoginType.KAKAO;
		String newUserProfile = "https://profile.com/new-image.jpg";

		User existingUser = User.builder()
			.kakaoId(kakaoId)
			.email(null)
			.nickname("기존닉네임")
			.password(null)
			.loginType(loginType)
			.userProfile("https://profile.com/old-image.jpg")
			.build();

		given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.of(existingUser));

		// when
		User result = socialAuthService.modifyOrJoin(kakaoId, email, newNickname, password, loginType, newUserProfile);

		// then
		assertThat(result).isSameAs(existingUser); // 같은 객체인지 확인
		assertThat(result.getNickname()).isEqualTo(newNickname);
		assertThat(result.getUserProfile()).isEqualTo(newUserProfile);

		verify(userRepository).findByKakaoId(kakaoId);
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	@DisplayName("사용자 ID로 토큰 생성 - 성공")
	void generateTokensById_Success() {
		// given
		Long userId = 1L;
		User user = User.builder()
			.kakaoId(123456789L)
			.email(null)
			.nickname("테스터")
			.password(null)
			.loginType(LoginType.KAKAO)
			.userProfile("https://profile.com/image.jpg")
			.build();

		// ID 설정 (중요!)
		ReflectionTestUtils.setField(user, "id", userId);

		JwtTokenDto expectedTokenDto = JwtTokenDto.builder()
			.grantType("Bearer")
			.accessToken("accessToken")
			.refreshToken("refreshToken")
			.accessTokenExpiresIn(3600L)
			.build();

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(authTokenService.generateTokens(user)).willReturn(expectedTokenDto);

		// when
		JwtTokenDto result = socialAuthService.generateTokensById(userId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getGrantType()).isEqualTo("Bearer");
		assertThat(result.getAccessToken()).isEqualTo("accessToken");
		assertThat(result.getRefreshToken()).isEqualTo("refreshToken");

		verify(userRepository).findById(userId);
		verify(authTokenService).generateTokens(user);
	}

	@Test
	@DisplayName("사용자 ID로 토큰 생성 - 사용자 없음")
	void generateTokensById_UserNotFound_ThrowsException() {
		// given
		Long userId = 999L;
		given(userRepository.findById(userId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> socialAuthService.generateTokensById(userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.USER_NOT_FOUND);

		verify(userRepository).findById(userId);
		verify(authTokenService, never()).generateTokens(any(User.class));
	}

	@Test
	@DisplayName("토큰에서 페이로드 추출 - 성공")
	void payloadOrNull_ValidToken_ReturnsPayload() {
		// given
		String accessToken = "validAccessToken";
		Map<String, Object> expectedPayload = Map.of(
			"id", 1L,
			"nickname", "테스터"
		);

		given(jwtTokenProvider.payloadOrNull(accessToken)).willReturn(expectedPayload);

		// when
		Map<String, Object> result = socialAuthService.payloadOrNull(accessToken);

		// then
		assertThat(result).isNotNull();
		assertThat(result.get("id")).isEqualTo(1L);
		assertThat(result.get("nickname")).isEqualTo("테스터");

		verify(jwtTokenProvider).payloadOrNull(accessToken);
	}

	@Test
	@DisplayName("토큰에서 페이로드 추출 - 유효하지 않은 토큰")
	void payloadOrNull_InvalidToken_ReturnsNull() {
		// given
		String accessToken = "invalidAccessToken";
		given(jwtTokenProvider.payloadOrNull(accessToken)).willReturn(null);

		// when
		Map<String, Object> result = socialAuthService.payloadOrNull(accessToken);

		// then
		assertThat(result).isNull();

		verify(jwtTokenProvider).payloadOrNull(accessToken);
	}
}
