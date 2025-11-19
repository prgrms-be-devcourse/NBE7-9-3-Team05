package com.back.motionit.domain.user.repository

import com.back.motionit.domain.user.dto.UserLoginProjection

interface UserRepositoryCustom {
    fun findLoginUserByEmail(email: String): UserLoginProjection?
}