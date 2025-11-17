package com.back.motionit.domain.auth.social.service

import com.back.motionit.domain.auth.service.AuthTokenService
import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.error.code.AuthErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.security.jwt.JwtTokenDto
import com.back.motionit.security.jwt.JwtTokenProvider
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.util.ReflectionTestUtils
import java.util.*
import java.util.Map

@ExtendWith(MockitoExtension::class)
@DisplayName("SocialAuthService 단위 테스트")
internal class SocialAuthServiceTest {
    @InjectMocks
    private lateinit var socialAuthService: SocialAuthService

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var authTokenService: AuthTokenService

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Test
    @DisplayName("카카오 신규 회원가입 - 성공")
    fun join_KakaoUser_Success() {

        val kakaoId = 123456789L
        val email: String? = null
        val nickname = "테스터"
        val password = ""
        val loginType = LoginType.KAKAO
        val userProfile = "https://profile.com/image.jpg"

        given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.empty())
        given(userRepository.existsByNickname(nickname)).willReturn(false)
        given(userRepository.save(any()))
            .willAnswer { invocation: InvocationOnMock -> invocation.getArgument(0) }

        val result = socialAuthService.modifyOrJoin(kakaoId, email, nickname, password, loginType, userProfile)

        assertThat(result).isNotNull()
        assertThat(result.kakaoId).isEqualTo(kakaoId)
        assertThat(result.email).isNull()
        assertThat(result.nickname).isEqualTo(nickname)
        assertThat(result.password).isNull()
        assertThat(result.loginType).isEqualTo(loginType)
        assertThat(result.userProfile).isEqualTo(userProfile)

        verify(userRepository).existsByNickname(nickname)
        verify(passwordEncoder, never()).encode(ArgumentMatchers.anyString())
        verify(userRepository).save(any())
    }

    @Test
    @DisplayName("카카오 회원가입 - 닉네임 중복으로 실패")
    fun join_NicknameDuplicated_ThrowsException() {

        val kakaoId = 123456789L
        val email: String? = null
        val nickname = "중복닉네임"
        val password = ""
        val loginType = LoginType.KAKAO
        val userProfile = "https://profile.com/image.jpg"

        given(userRepository.existsByNickname(nickname)).willReturn(true)

        assertThatThrownBy {
            socialAuthService.modifyOrJoin(
                kakaoId,
                email,
                nickname,
                password,
                loginType,
                userProfile
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.NICKNAME_DUPLICATED)

        verify(userRepository).existsByNickname(nickname)
        verify(userRepository, never()).save(any())
    }

    @Test
    @DisplayName("기존 카카오 회원 정보 수정")
    fun modifyOrJoin_ExistingKakaoUser_UpdatesUser() {

        val kakaoId = 123456789L
        val email: String? = null
        val newNickname = "새닉네임"
        val password = ""
        val loginType = LoginType.KAKAO
        val newUserProfile = "https://profile.com/new-image.jpg"

        val existingUser = User.builder()
            .kakaoId(kakaoId)
            .email(null)
            .nickname("기존닉네임")
            .password(null)
            .loginType(loginType)
            .userProfile("https://profile.com/old-image.jpg")
            .build()

        given(userRepository.findByKakaoId(kakaoId)).willReturn(Optional.of(existingUser))

        val result = socialAuthService.modifyOrJoin(kakaoId, email, newNickname, password, loginType, newUserProfile)

        assertThat(result).isSameAs(existingUser) // 같은 객체인지 확인
        assertThat(result.nickname).isEqualTo(newNickname)
        assertThat(result.userProfile).isEqualTo(newUserProfile)

        verify(userRepository).findByKakaoId(kakaoId)
        verify(userRepository, never()).save(any())
    }

    @Test
    @DisplayName("사용자 ID로 토큰 생성 - 성공")
    fun generateTokensById_Success() {

        val userId = 1L
        val user = User.builder()
            .kakaoId(123456789L)
            .email(null)
            .nickname("테스터")
            .password(null)
            .loginType(LoginType.KAKAO)
            .userProfile("https://profile.com/image.jpg")
            .build()

        ReflectionTestUtils.setField(user, "id", userId)

        val expectedTokenDto = JwtTokenDto.builder()
            .grantType("Bearer")
            .accessToken("accessToken")
            .refreshToken("refreshToken")
            .accessTokenExpiresIn(3600L)
            .build()

        given(userRepository.findById(userId)).willReturn(Optional.of(user))
        given(authTokenService.generateTokens(user)).willReturn(expectedTokenDto)

        val result = socialAuthService!!.generateTokensById(userId)

        assertThat(result).isNotNull()
        assertThat(result.grantType).isEqualTo("Bearer")
        assertThat(result.accessToken).isEqualTo("accessToken")
        assertThat(result.refreshToken).isEqualTo("refreshToken")

        verify(userRepository).findById(userId)
        verify(authTokenService).generateTokens(user)
    }

    @Test
    @DisplayName("사용자 ID로 토큰 생성 - 사용자 없음")
    fun generateTokensById_UserNotFound_ThrowsException() {

        val userId = 999L
        given(userRepository.findById(userId)).willReturn(Optional.empty())

        Assertions.assertThatThrownBy { socialAuthService.generateTokensById(userId) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.USER_NOT_FOUND)

        verify(userRepository).findById(userId)
        verify(authTokenService, never()).generateTokens(any())
    }

    @Test
    @DisplayName("토큰에서 페이로드 추출 - 성공")
    fun payloadOrNull_ValidToken_ReturnsPayload() {

        val accessToken = "validAccessToken"
        val expectedPayload = Map.of<String, Any?>(
            "id", 1L,
            "nickname", "테스터"
        )

        given(jwtTokenProvider.payloadOrNull(accessToken)).willReturn(expectedPayload)

        val result = socialAuthService.payloadOrNull(accessToken)

        assertThat(result).isNotNull()
        assertThat(result!!["id"]).isEqualTo(1L)
        assertThat(result["nickname"]).isEqualTo("테스터")

        verify(jwtTokenProvider).payloadOrNull(accessToken)
    }

    @Test
    @DisplayName("토큰에서 페이로드 추출 - 유효하지 않은 토큰")
    fun payloadOrNull_InvalidToken_ReturnsNull() {

        val accessToken = "invalidAccessToken"
        given(jwtTokenProvider.payloadOrNull(accessToken)).willReturn(null)

        val result = socialAuthService.payloadOrNull(accessToken)

        assertThat(result).isNull()

        verify(jwtTokenProvider).payloadOrNull(accessToken)
    }
}
