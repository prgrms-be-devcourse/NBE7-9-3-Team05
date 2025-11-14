package com.back.motionit.security.jwt

import com.back.motionit.domain.user.entity.User
import com.back.motionit.standard.ut.JwtUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secret: String,

    @Value("\${jwt.access-token-expiration}")
    val accessTokenExpiration: Long,

    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long
) {

    fun generateAccessToken(user: User): String {
        return JwtUtil.Jwt.toString(
            secret,
            accessTokenExpiration,
            java.util.Map.of<String, Any?>("id", user.id, "nickname", user.nickname)
        )
    }

    fun generateRefreshToken(user: User): String {
        return JwtUtil.Jwt.toString(
            secret,
            refreshTokenExpiration,
            java.util.Map.of<String, Any?>("id", user.id, "nickname", user.nickname)
        )
    }

    fun payloadOrNull(jwt: String?): Map<String, Any?>? {
        val payload = JwtUtil.Jwt.payloadOrNull(jwt, secret) ?: return null

        val idNo = payload["id"] as Number?
        val id = idNo!!.toLong()

        val nickname = payload["nickname"] as String?

        return java.util.Map.of<String, Any?>("id", id, "nickname", nickname)
    }

    fun isExpired(jwt: String?): Boolean {
        return JwtUtil.Jwt.isExpired(jwt, secret)
    }
}
