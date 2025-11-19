package com.back.motionit.domain.user.service

import com.back.motionit.domain.user.dto.UpdateUserProfileRequest
import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.error.code.AuthErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.global.service.AwsCdnSignService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.ObjectProvider
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var cdnProvider: ObjectProvider<AwsCdnSignService>

    @Mock
    lateinit var cdnSignService: AwsCdnSignService

    @InjectMocks
    lateinit var userService: UserService

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .nickname("testUser")
            .password("password123")
            .loginType(LoginType.LOCAL)
            .userProfile("profiles/test.png")
            .build()
    }

    @Nested
    @DisplayName("getUserProfile - 사용자 프로필 조회")
    internal inner class GetUserProfileTest {

        @Test
        @DisplayName("성공 - S3 ObjectKey가 있는 경우 CDN 서명 URL 생성")
        fun getUserProfile_withS3Key_success() {
            // given
            val userId = 1L
            val signedUrl = "https://cdn.example.com/profiles/test.png?signed=true"

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser))
            given(cdnProvider.getIfAvailable()).willReturn(cdnSignService)
            given(cdnSignService.sign("profiles/test.png")).willReturn(signedUrl)

            // when
            val result = userService.getUserProfile(userId)

            // then
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.email).isEqualTo("test@example.com")
            assertThat(result.nickname).isEqualTo("testUser")
            assertThat(result.userProfileUrl).isEqualTo(signedUrl)
            assertThat(result.loginType).isEqualTo(LoginType.LOCAL)

            verify(userRepository).findById(userId)
            verify(cdnSignService).sign("profiles/test.png")
        }

        @Test
        @DisplayName("성공 - 외부 URL인 경우 그대로 반환")
        fun getUserProfile_withExternalUrl_success() {
            // given
            val userId = 1L
            val externalUrl = "https://external.com/profile.jpg"
            testUser.userProfile = externalUrl

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser))

            // when
            val result = userService.getUserProfile(userId)

            // then
            assertThat(result.userProfileUrl).isEqualTo(externalUrl)
            verify(userRepository).findById(userId)
            verify(cdnProvider, never()).getIfAvailable()
        }

        @Test
        @DisplayName("성공 - 프로필 이미지가 null인 경우")
        fun getUserProfile_withNullProfile_success() {
            // given
            val userId = 1L
            testUser.userProfile = null

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser))

            // when
            val result = userService.getUserProfile(userId)

            // then
            assertThat(result.userProfileUrl).isNull()
            verify(userRepository).findById(userId)
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        fun getUserProfile_userNotFound_throwsException() {
            // given
            val userId = 999L
            given(userRepository.findById(userId)).willReturn(Optional.empty())

            // when & then
            assertThatThrownBy { userService.getUserProfile(userId) }
                .isInstanceOf(BusinessException::class.java)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.USER_NOT_FOUND)

            verify(userRepository).findById(userId)
        }
    }

    @Nested
    @DisplayName("updateUserProfile - 사용자 프로필 수정")
    internal inner class UpdateUserProfileTest {

        @Test
        @DisplayName("성공 - 닉네임만 수정")
        fun updateUserProfile_nicknameOnly_success() {
            // given
            val userId = 1L
            val request = UpdateUserProfileRequest(
                nickname = "newNickname",
                userProfile = null
            )
            val signedUrl = "https://cdn.example.com/profiles/test.png?signed=true"

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser))
            given(userRepository.existsByNickname("newNickname")).willReturn(false)
            given(cdnProvider.getIfAvailable()).willReturn(cdnSignService)
            given(cdnSignService.sign("profiles/test.png")).willReturn(signedUrl)

            // when
            val result = userService.updateUserProfile(userId, request)

            // then
            assertThat(result.nickname).isEqualTo("newNickname")
            assertThat(testUser.nickname).isEqualTo("newNickname")
            assertThat(testUser.userProfile).isEqualTo("profiles/test.png")

            verify(userRepository).findById(userId)
            verify(userRepository).existsByNickname("newNickname")
        }

        @Test
        @DisplayName("성공 - 프로필 이미지만 수정")
        fun updateUserProfile_imageOnly_success() {
            // given
            val userId = 1L
            val request = UpdateUserProfileRequest(
                nickname = null,
                userProfile = "profiles/new-image.png"
            )
            val signedUrl = "https://cdn.example.com/profiles/new-image.png?signed=true"

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser))
            given(cdnProvider.getIfAvailable()).willReturn(cdnSignService)
            given(cdnSignService.sign("profiles/new-image.png")).willReturn(signedUrl)

            // when
            val result = userService.updateUserProfile(userId, request)

            // then
            assertThat(result.nickname).isEqualTo("testUser")
            assertThat(testUser.userProfile).isEqualTo("profiles/new-image.png")
            assertThat(result.userProfileUrl).isEqualTo(signedUrl)

            verify(userRepository).findById(userId)
            verify(userRepository, never()).existsByNickname(anyString())
        }

        @Test
        @DisplayName("성공 - 닉네임과 프로필 이미지 모두 수정")
        fun updateUserProfile_both_success() {
            // given
            val userId = 1L
            val request = UpdateUserProfileRequest(
                nickname = "updatedNickname",
                userProfile = "profiles/updated-image.png"
            )
            val signedUrl = "https://cdn.example.com/profiles/updated-image.png?signed=true"

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser))
            given(userRepository.existsByNickname("updatedNickname")).willReturn(false)
            given(cdnProvider.getIfAvailable()).willReturn(cdnSignService)
            given(cdnSignService.sign("profiles/updated-image.png")).willReturn(signedUrl)

            // when
            val result = userService.updateUserProfile(userId, request)

            // then
            assertThat(result.nickname).isEqualTo("updatedNickname")
            assertThat(result.userProfileUrl).isEqualTo(signedUrl)
            assertThat(testUser.nickname).isEqualTo("updatedNickname")
            assertThat(testUser.userProfile).isEqualTo("profiles/updated-image.png")

            verify(userRepository).findById(userId)
            verify(userRepository).existsByNickname("updatedNickname")
        }

        @Test
        @DisplayName("성공 - 동일한 닉네임으로 수정 (중복 체크 안함)")
        fun updateUserProfile_sameNickname_success() {
            // given
            val userId = 1L
            val request = UpdateUserProfileRequest(
                nickname = "testUser", // 동일한 닉네임
                userProfile = null
            )
            val signedUrl = "https://cdn.example.com/profiles/test.png?signed=true"

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser))
            given(cdnProvider.getIfAvailable()).willReturn(cdnSignService)
            given(cdnSignService.sign("profiles/test.png")).willReturn(signedUrl)

            // when
            val result = userService.updateUserProfile(userId, request)

            // then
            assertThat(result.nickname).isEqualTo("testUser")
            verify(userRepository).findById(userId)
            verify(userRepository, never()).existsByNickname(anyString())
        }

        @Test
        @DisplayName("성공 - 둘 다 null인 경우 변경 없음")
        fun updateUserProfile_bothNull_noChange() {
            // given
            val userId = 1L
            val request = UpdateUserProfileRequest(
                nickname = null,
                userProfile = null
            )
            val originalNickname = testUser.nickname
            val originalProfile = testUser.userProfile
            val signedUrl = "https://cdn.example.com/profiles/test.png?signed=true"

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser))
            given(cdnProvider.getIfAvailable()).willReturn(cdnSignService)
            given(cdnSignService.sign("profiles/test.png")).willReturn(signedUrl)

            // when
            val result = userService.updateUserProfile(userId, request)

            // then
            assertThat(testUser.nickname).isEqualTo(originalNickname)
            assertThat(testUser.userProfile).isEqualTo(originalProfile)

            verify(userRepository).findById(userId)
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        fun updateUserProfile_userNotFound_throwsException() {
            // given
            val userId = 999L
            val request = UpdateUserProfileRequest(
                nickname = "newNickname",
                userProfile = null
            )

            given(userRepository.findById(userId)).willReturn(Optional.empty())

            // when & then
            assertThatThrownBy { userService.updateUserProfile(userId, request) }
                .isInstanceOf(BusinessException::class.java)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.USER_NOT_FOUND)

            verify(userRepository).findById(userId)
        }

        @Test
        @DisplayName("실패 - 중복된 닉네임")
        fun updateUserProfile_duplicateNickname_throwsException() {
            // given
            val userId = 1L
            val request = UpdateUserProfileRequest(
                nickname = "duplicatedNickname",
                userProfile = null
            )

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser))
            given(userRepository.existsByNickname("duplicatedNickname")).willReturn(true)

            // when & then
            assertThatThrownBy { userService.updateUserProfile(userId, request) }
                .isInstanceOf(BusinessException::class.java)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.NICKNAME_DUPLICATED)

            verify(userRepository).findById(userId)
            verify(userRepository).existsByNickname("duplicatedNickname")
        }
    }

    @Nested
    @DisplayName("generateProfileUrl - 프로필 URL 생성")
    internal inner class GenerateProfileUrlTest {

        @Test
        @DisplayName("성공 - http:// 로 시작하는 외부 URL")
        fun generateProfileUrl_httpUrl() {
            // given
            val userId = 1L
            val httpUrl = "http://external.com/profile.jpg"
            testUser.userProfile = httpUrl

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser))

            // when
            val result = userService.getUserProfile(userId)

            // then
            assertThat(result.userProfileUrl).isEqualTo(httpUrl)
        }

        @Test
        @DisplayName("성공 - https:// 로 시작하는 외부 URL")
        fun generateProfileUrl_httpsUrl() {
            // given
            val userId = 1L
            val httpsUrl = "https://external.com/profile.jpg"
            testUser.userProfile = httpsUrl

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser))

            // when
            val result = userService.getUserProfile(userId)

            // then
            assertThat(result.userProfileUrl).isEqualTo(httpsUrl)
        }

        @Test
        @DisplayName("성공 - CDN 서비스가 없는 경우 빈 문자열 반환")
        fun generateProfileUrl_noCdnService_returnsEmpty() {
            // given
            val userId = 1L
            testUser.userProfile = "profiles/test.png"

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser))
            given(cdnProvider.getIfAvailable()).willReturn(null)

            // when
            val result = userService.getUserProfile(userId)

            // then
            assertThat(result.userProfileUrl).isEmpty()
        }
    }
}
