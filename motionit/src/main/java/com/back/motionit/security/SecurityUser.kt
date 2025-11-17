package com.back.motionit.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.core.user.OAuth2User

class SecurityUser(
    val id: Long,
    password: String,
    val nickname: String,
    authorities: Collection<GrantedAuthority>
) : User(nickname, password, authorities), OAuth2User {

    override fun getAttributes(): Map<String, Any> {
        return java.util.Map.of()
    }

    override fun getName(): String = nickname
}
