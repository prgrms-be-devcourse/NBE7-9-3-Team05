package com.back.motionit.domain.user.dto

import com.querydsl.core.annotations.QueryProjection

data class UserLoginProjection @QueryProjection constructor(
    val id: Long,
    val email: String,
    val password: String,
    val nickname: String
)
