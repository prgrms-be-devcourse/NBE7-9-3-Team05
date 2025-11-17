package com.back.motionit.domain.auth

import com.back.motionit.domain.auth.social.service.SocialAuthService
import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.security.jwt.JwtTokenProvider
import jakarta.persistence.EntityManager
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("소셜로그인 통합 테스트")
internal class SocialLoginIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var socialAuthService: SocialAuthService

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("소셜 로그인 통합 테스트 - CustomOAuth2UserService 호출 → DB 저장 → 토큰 발급 → /api/v1/users/profile 접근 성공")
    fun completeE2E_DirectServiceCall() {
        // STEP 1: OAuth2User 생성
        val kakaoId = 123456789L
        val nickname = "테스트유저"
        val profileImage = "https://k.kakaocdn.net/profile.jpg"

        val oauth2User = createOAuth2User(kakaoId, nickname, profileImage)
        val attributes = oauth2User.attributes
        val extractedKakaoId = attributes["id"].toString().toLong()
        val properties = attributes["properties"] as Map<*, *>

        // STEP 2: 회원 가입 or 수정
        socialAuthService.modifyOrJoin(
            extractedKakaoId,
            null,
            (properties["nickname"] as String),
            "",
            LoginType.KAKAO,
            (properties["profile_image"] as String)
        )

        // STEP 3: 저장 검증
        entityManager.flush()
        entityManager.clear()

        val savedUser = userRepository.findByKakaoId(kakaoId).orElseThrow()

        assertThat(savedUser.nickname).isEqualTo(nickname)
        assertThat(savedUser.userProfile).isEqualTo(profileImage)

        // STEP 4: JWT 발급
        val tokens = socialAuthService.generateTokensById(savedUser.id!!)
        assertThat(tokens.accessToken).isNotBlank()
        assertThat(tokens.refreshToken).isNotBlank()

        // STEP 5: Payload 검증
        val payload = jwtTokenProvider.payloadOrNull(tokens.accessToken)
        assertThat(payload!!["id"]).isEqualTo(savedUser.id)
        assertThat(payload["nickname"]).isEqualTo(nickname)

        // STEP 6: 실제 보호 리소스 접근 (쿠키)
        mockMvc.perform(
            get("/api/v1/users/profile")
                .cookie(Cookie("accessToken", tokens.accessToken))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk())

        // STEP 7: 헤더 인증도 가능
        mockMvc.perform(
            get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + tokens.accessToken)
        )
            .andExpect(status().isOk())
    }

    @Test
    @DisplayName("신규 사용자 가입 → 토큰 발급 → 인증 성공")
    fun newUserFlow_Complete() {
        val kakaoId = 111222333L
        val nickname = "신규유저"
        val profile = "profile.jpg"

        val user = socialAuthService.modifyOrJoin(
            kakaoId, null, nickname, "", LoginType.KAKAO, profile
        )

        assertThat(user.id).isNotNull()

        val tokens = socialAuthService.generateTokensById(user.id!!)
        assertThat(tokens.accessToken).isNotBlank()

        val payload = socialAuthService.payloadOrNull(tokens.accessToken)
        assertThat(payload!!["nickname"]).isEqualTo(nickname)
    }

    @Test
    @DisplayName("기존 사용자 재로그인 시 update() 동작")
    fun existingUserRelogin_UpdatesInfo() {
        val kakaoId = 444555666L

        val old = socialAuthService.modifyOrJoin(
            kakaoId, null, "기존닉", "", LoginType.KAKAO, "old.png"
        )

        entityManager.flush()
        entityManager.clear()

        val updated = socialAuthService.modifyOrJoin(
            kakaoId, null, "변경닉", "", LoginType.KAKAO, "new.png"
        )

        assertThat(updated.id).isEqualTo(old.id)
        assertThat(updated.nickname).isEqualTo("변경닉")
        assertThat(updated.userProfile).isEqualTo("new.png")
    }

    @Test
    @DisplayName("로그인 → 보호 API 접근 → 로그아웃 → refreshToken 제거")
    fun loginAccessLogout_Complete() {
        val user = socialAuthService.modifyOrJoin(
            999888L, null, "로그아웃유저", "", LoginType.KAKAO, "profile.jpg"
        )

        val tokens = socialAuthService.generateTokensById(user.id!!)

        // 접근 성공
        mockMvc.perform(
            get("/api/v1/users/profile")
                .cookie(Cookie("accessToken", tokens.accessToken))
        )
            .andExpect(status().isOk())

        // 로그아웃 처리 → refreshToken 제거
        val find = userRepository.findById(user.id!!).orElseThrow()
        find.removeRefreshToken()
        entityManager.flush()

        val loggedOut = userRepository.findById(user.id!!).orElseThrow()
        assertThat(loggedOut.refreshToken).isNull()

        // AccessToken은 여전히 유효 → 접근 OK
        mockMvc.perform(
            get("/api/v1/users/profile")
                .cookie(Cookie("accessToken", tokens.accessToken))
        )
            .andExpect(status().isOk())
    }

    @Test
    @DisplayName("쿠키 기반 인증 성공")
    fun customAuthenticationFilter_CookieBased() {
        val user = socialAuthService.modifyOrJoin(
            777777L, null, "필터쿠키", "", LoginType.KAKAO, "p.jpg"
        )
        val tokens = socialAuthService.generateTokensById(user.id!!)

        mockMvc.perform(
            get("/api/v1/users/profile")
                .cookie(Cookie("accessToken", tokens.accessToken))
        )
            .andExpect(status().isOk())
    }

    @Test
    @DisplayName("Authorization 헤더 인증 성공")
    fun customAuthenticationFilter_HeaderBased() {
        val user = socialAuthService.modifyOrJoin(
            888888L, null, "필터헤더", "", LoginType.KAKAO, "p.jpg"
        )
        val tokens = socialAuthService.generateTokensById(user.id!!)

        mockMvc.perform(
            get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + tokens.accessToken)
        )
            .andExpect(status().isOk())
    }

    @Test
    @DisplayName("토큰 없이 접근 → 401")
    fun customAuthenticationFilter_NoToken_Returns401() {
        mockMvc.perform(get("/api/v1/users/profile"))
            .andExpect(status().isUnauthorized())
    }

    @Test
    @DisplayName("잘못된 토큰 → 401")
    fun customAuthenticationFilter_InvalidToken_Returns401() {
        mockMvc.perform(
            get("/api/v1/users/profile")
                .header("Authorization", "Bearer invalid.token")
        )
            .andExpect(status().isUnauthorized())
    }


    // Helper Method
    private fun createOAuth2User(kakaoId: Long, nickname: String, profileImage: String): OAuth2User {
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["id"] = kakaoId

        val properties: MutableMap<String, Any> = HashMap()
        properties["nickname"] = nickname
        properties["profile_image"] = profileImage
        attributes["properties"] = properties

        attributes["kakao_account"] = HashMap<Any, Any>()

        return DefaultOAuth2User(
            listOf(SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "id"
        )
    }
}
