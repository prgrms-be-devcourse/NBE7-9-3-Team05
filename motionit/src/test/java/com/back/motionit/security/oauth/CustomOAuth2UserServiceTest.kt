package com.back.motionit.security.oauth

import com.back.motionit.domain.auth.social.service.SocialAuthService
import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.domain.user.entity.User.Companion.builder
import com.back.motionit.security.SecurityUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.test.util.ReflectionTestUtils.setField

@ExtendWith(MockitoExtension::class)
internal class CustomOAuth2UserServiceTest {

    @Mock
    private lateinit var socialAuthService: SocialAuthService

    @Test
    @DisplayName("카카오 attributes 파싱 → modifyOrJoin 호출 → SecurityUser 생성")
    fun loadUser_InternalLogic_Success() {

        val kakaoId = 12345L
        val nickname = "testUser"
        val profileImage = "https://image.jpg"

        val attributes = mapOf(
            "id" to kakaoId,
            "properties" to mapOf(
                "nickname" to nickname,
                "profile_image" to profileImage
            ),
            "kakao_account" to emptyMap<String, Any>()
        )

        val fakeOAuthUser: OAuth2User = DefaultOAuth2User(
            listOf(SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "id"
        )

        val user = builder()
            .kakaoId(kakaoId)
            .nickname(nickname)
            .userProfile(profileImage)
            .loginType(LoginType.KAKAO)
            .build()
        setField(user, "id", 1L)

        given(socialAuthService.modifyOrJoin(kakaoId, null, nickname, "", LoginType.KAKAO, profileImage))
            .willReturn(user)


        val oAuth2User = fakeOAuthUser

        val parsedId = oAuth2User.name.toLong()
        val props = oAuth2User.attributes["properties"] as? Map<*, *>
            ?: emptyMap<String, Any>()

        val parsedNickname = props["nickname"] as? String
        val parsedProfile = props["profile_image"] as? String

        val resultUser = socialAuthService.modifyOrJoin(
            parsedId, null, parsedNickname!!, "", LoginType.KAKAO, parsedProfile!!
        )

        val pw =
            if ((resultUser.password != null && resultUser.password!!.isNotEmpty())
            ) resultUser.password
            else "OAUTH2_USER"

        val securityUser = SecurityUser(
            resultUser.id!!,
            pw!!,
            resultUser.nickname,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )


        assertThat(securityUser.id).isEqualTo(1L)
        assertThat(securityUser.nickname).isEqualTo("testUser")
        assertThat(securityUser.password).isEqualTo("OAUTH2_USER")
        assertThat(securityUser.authorities).extracting("authority")
            .containsExactly("ROLE_USER")

        verify(socialAuthService).modifyOrJoin(
            kakaoId, null, nickname, "", LoginType.KAKAO, profileImage
        )
    }
}
