package com.back.motionit.security.jwt

data class JwtTokenDto(
    val grantType: String,
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long
) {
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var grantType: String? = null
        private var accessToken: String? = null
        private var refreshToken: String? = null
        private var accessTokenExpiresIn: Long? = null

        fun grantType(grantType: String) = apply { this.grantType = grantType }
        fun accessToken(accessToken: String) = apply { this.accessToken = accessToken }
        fun refreshToken(refreshToken: String) = apply { this.refreshToken = refreshToken }
        fun accessTokenExpiresIn(value: Long) = apply { this.accessTokenExpiresIn = value }

        fun build(): JwtTokenDto {
            return JwtTokenDto(
                grantType = grantType!!,
                accessToken = accessToken!!,
                refreshToken = refreshToken!!,
                accessTokenExpiresIn = accessTokenExpiresIn!!
            )
        }
    }
}
