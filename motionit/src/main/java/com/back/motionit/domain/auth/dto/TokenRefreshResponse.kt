package com.back.motionit.domain.auth.dto

data class TokenRefreshResponse (
    val accessToken: String,
    val expiresIn: Long
)
