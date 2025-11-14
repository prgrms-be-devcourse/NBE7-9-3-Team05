package com.back.motionit.domain.user.dto

import com.back.motionit.domain.user.entity.LoginType

data class UserProfileResponse(
    val userId: Long,
    val email: String?,
    val nickname: String,
    val userProfileUrl: String?,
    val loginType: LoginType
) {
    companion object {
        @JvmStatic
        fun builder(): UserProfileResponseBuilder = UserProfileResponseBuilder()
    }

    class UserProfileResponseBuilder {
        private var userId: Long = 0
        private var email: String? = null
        private var nickname: String = ""
        private var userProfileUrl: String? = null
        private var loginType: LoginType = LoginType.LOCAL

        fun userId(userId: Long) = apply { this.userId = userId }
        fun email(email: String?) = apply { this.email = email }
        fun nickname(nickname: String) = apply { this.nickname = nickname }
        fun userProfileUrl(userProfileUrl: String?) = apply { this.userProfileUrl = userProfileUrl }
        fun loginType(loginType: LoginType) = apply { this.loginType = loginType }

        fun build() = UserProfileResponse(userId, email, nickname, userProfileUrl, loginType)
    }
}
