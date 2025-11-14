package com.back.motionit.standard.ut

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import java.security.Key
import java.util.*

class JwtUtil {
    object Jwt {
        @JvmStatic
        fun toString(secret: String, expireSeconds: Long, body: Map<String, Any>): String {
            val claimsBuilder = Jwts.claims()

            for ((key, value) in body) {
                claimsBuilder.add(key, value)
            }

            val claims = claimsBuilder.build()

            val issuedAt = Date()
            val expiration = Date(issuedAt.time + 1000L * expireSeconds)

            val secretKey: Key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret.trim { it <= ' ' }))

            val jwt = Jwts.builder()
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact()

            return jwt
        }

        @JvmStatic
        fun isValid(jwt: String?, secret: String): Boolean {
            val secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret.trim { it <= ' ' }))

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
        fun payloadOrNull(jwt: String?, secret: String): Map<String, Any>? {
            val secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret.trim { it <= ' ' }))

            if (isValid(jwt, secret)) {
                return Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(jwt)
                    .payload as Map<String, Any>
            }

            return null
        }

        @JvmStatic
        fun isExpired(jwt: String?, secret: String): Boolean {
            val secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret.trim { it <= ' ' }))

            try {
                Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(jwt)
                return false
            } catch (e: ExpiredJwtException) {
                return true
            } catch (e: Exception) {
                return true
            }
        }
    }
}
