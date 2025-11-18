package com.back.motionit.standard.ut

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import java.util.*
import javax.crypto.SecretKey

class JwtUtil {
    object Jwt {

        @JvmStatic
        fun toString(secretKey: SecretKey, expireSeconds: Long, body: Map<String, Any>): String {

            val claims = Jwts.claims().apply {
                body.forEach { (key, value) -> add(key, value) }
            }.build()

            val issuedAt = Date()
            val expiration = Date(issuedAt.time + 1000L * expireSeconds)

            return Jwts.builder()
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact()
        }

        @JvmStatic
        fun isValid(jwt: String, secretKey: SecretKey): Boolean {

            try {
                Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(jwt)
            } catch (e: Exception) {
                return false
            }

            return true
        }

        @JvmStatic
        fun payloadOrNull(jwt: String?, secretKey: SecretKey): Map<String, Any?>? {

            if (jwt.isNullOrBlank()) return null
            if (!isValid(jwt, secretKey)) return null


            val parsed = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parse(jwt)

            val claims = parsed.payload as? io.jsonwebtoken.Claims ?: return null

            return claims


        }

        @JvmStatic
        fun isExpired(jwt: String?, secretKey: SecretKey): Boolean {
            if (jwt.isNullOrBlank()) return true

            return try {
                Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(jwt)
                false
            } catch (e: ExpiredJwtException) {
                true
            } catch (e: Exception) {
                true
            }
        }
    }
}
