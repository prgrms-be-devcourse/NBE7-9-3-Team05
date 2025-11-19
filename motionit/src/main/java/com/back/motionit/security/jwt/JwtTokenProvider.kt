package com.back.motionit.security.jwt

import com.back.motionit.domain.user.entity.User
import com.back.motionit.standard.ut.JwtUtil
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secret: String,

    @Value("\${jwt.access-token-expiration}")
    val accessTokenExpiration: Long,

    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long
) {

    private val secretKey: SecretKey =
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret.trim()))

    fun generateAccessToken(user: User): String {
        return JwtUtil.Jwt.toString(
            secretKey,
            accessTokenExpiration,
            mapOf<String, Any>("id" to user.id!!, "nickname" to user.nickname)
        )
    }

    fun generateRefreshToken(user: User): String {
        return JwtUtil.Jwt.toString(
            secretKey,
            refreshTokenExpiration,
            mapOf<String, Any>("id" to user.id!!, "nickname" to user.nickname)
        )
    }

    fun payloadOrNull(jwt: String?): Map<String, Any?>? {
        val payload = JwtUtil.Jwt.payloadOrNull(jwt, secretKey) ?: return null

        val idNo = payload["id"] as Number?
        val id = idNo!!.toLong()

        val nickname = payload["nickname"] as String?

        return mapOf("id" to id, "nickname" to nickname)
    }

    fun isExpired(jwt: String?): Boolean =
        JwtUtil.Jwt.isExpired(jwt, secretKey)
}
