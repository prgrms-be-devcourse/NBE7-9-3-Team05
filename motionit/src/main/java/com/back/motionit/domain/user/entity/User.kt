package com.back.motionit.domain.user.entity

import com.back.motionit.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_login_type_email",
            columnNames = ["login_type", "email"]
        ),
        UniqueConstraint(
            name = "uk_login_type_kakao_id",
            columnNames = ["login_type", "kakao_id"]
        )
    ]
)
class User(
    @Column(name = "kakao_id")
    var kakaoId: Long? = null,

    @Column(length = 100)
    var email: String? = null,

    @Column(nullable = false, length = 50)
    var nickname: String,

    @Column(length = 255)
    var password: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false, length = 20)
    var loginType: LoginType,

    @Column(name = "user_profile", length = 500)
    var userProfile: String? = null
) : BaseEntity() {

    @Column(name = "refresh_token", length = 500)
    var refreshToken: String? = null

    // 테스트용 생성자 (id, nickname만 받는 경우)
    constructor(id: Long?, nickname: String) : this(
        kakaoId = null,
        email = null,
        nickname = nickname,
        password = null,
        loginType = LoginType.LOCAL,
        userProfile = null
    ) {
        this.id = id
    }

    fun updateRefreshToken(refreshToken: String?) {
        this.refreshToken = refreshToken
    }

    fun removeRefreshToken() {
        this.refreshToken = null
    }

    fun updatePassword(password: String?) {
        this.password = password
    }

    fun update(nickname: String, userProfile: String?) {
        this.nickname = nickname
        this.userProfile = userProfile
    }

    companion object {
        @JvmStatic
        fun builder(): UserBuilder = UserBuilder()
    }

    class UserBuilder {
        private var kakaoId: Long? = null
        private var email: String? = null
        private var nickname: String = ""
        private var password: String? = null
        private var loginType: LoginType = LoginType.LOCAL
        private var userProfile: String? = null

        fun kakaoId(kakaoId: Long?) = apply { this.kakaoId = kakaoId }
        fun email(email: String?) = apply { this.email = email }
        fun nickname(nickname: String) = apply { this.nickname = nickname }
        fun password(password: String?) = apply { this.password = password }
        fun loginType(loginType: LoginType) = apply { this.loginType = loginType }
        fun userProfile(userProfile: String?) = apply { this.userProfile = userProfile }

        fun build() = User(kakaoId, email, nickname, password, loginType, userProfile)
    }
}
