package com.back.motionit.domain.auth.dto

data class AuthResponse (
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val email: String? = null,
    val nickname: String
)
