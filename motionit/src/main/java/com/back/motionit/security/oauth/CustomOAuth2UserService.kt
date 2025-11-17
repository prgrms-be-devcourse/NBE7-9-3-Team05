package com.back.motionit.security.oauth

import com.back.motionit.domain.auth.social.service.SocialAuthService
import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.security.SecurityUser
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomOAuth2UserService(
    private val socialAuthService: SocialAuthService
) : DefaultOAuth2UserService() {

    // 카카오 회원 정보 수정시 db 반영을 위한 더티체킹
    @Transactional
    @Throws(OAuth2AuthenticationException::class)
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)

        val kakaoId = oAuth2User.name.toLong()

        val attributes = oAuth2User.attributes
        val attributesProperties = attributes["properties"] as? Map<*, *> ?: emptyMap<Any, Any>()

        val nickname = attributesProperties["nickname"] as String?
        val userProfile = attributesProperties["profile_image"] as String?
        val password = ""
        val email: String? = null
        val loginType = LoginType.KAKAO

        val user = socialAuthService.modifyOrJoin(kakaoId, email, nickname!!, password, loginType, userProfile!!)

        // 최소 권한 부여
        val authorities: Collection<GrantedAuthority> = listOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_USER"))

        // OAuth2 사용자는 패스워드가 null일 수 있으므로 기본값 설정
        val userPassword = if ((user.password != null && user.password!!.isNotEmpty())
        ) user.password
        else "OAUTH2_USER"

        return SecurityUser(
            user.id!!,
            userPassword!!,
            user.nickname,
            authorities
        )
    }
}
