package com.back.motionit.global.request

import com.back.motionit.domain.user.entity.User
import com.back.motionit.global.error.code.AuthErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.security.SecurityUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class RequestContext(
    private val request: HttpServletRequest,
    private val response: HttpServletResponse,

    @Value("\${cookie.domain:localhost}")
    private val cookieDomain: String,

    @Value("\${jwt.refresh-token-expiration:1209600}")
    private val refreshTokenExpiration: Long,

    @Value("\${jwt.access-token-expiration:3600}")
    private val accessTokenExpiration: Long
) {

    val actor: User
        get() {
            val auth: Authentication? = SecurityContextHolder.getContext().authentication
            val principal = auth?.principal

            if (principal is SecurityUser) {
                return User(principal.id, principal.nickname)
            }
            throw BusinessException(AuthErrorCode.UNAUTHORIZED)
        }

    fun setHeader(name: String, value: String) {
        response.setHeader(name, value)
    }

    fun getHeader(name: String, defaultValue: String): String {
        val headerValue = request.getHeader(name)
        return if (headerValue != null && headerValue.isNotBlank()) headerValue else defaultValue
    }

    fun getCookieValue(name: String, defaultValue: String): String {
        val cookies = request.cookies ?: return defaultValue

        for (cookie in cookies) {
            if (cookie.name == name) {
                val value = cookie.value
                if (value != null && value.isNotBlank()) {
                    return value
                }
                break
            }
        }

        return defaultValue
    }

    fun setCookie(name: String, value: String?) {
        val cookieValue = value ?: ""

        val cookie = Cookie(name, cookieValue)
        cookie.path = "/"
        cookie.isHttpOnly = true

        val isLocalhostDomain =
            cookieDomain.isBlank() ||
                    cookieDomain.equals("localhost", ignoreCase = true) ||
                    cookieDomain == "127.0.0.1"

        if (!isLocalhostDomain) {
            cookie.domain = cookieDomain
        }

        cookie.secure = request.isSecure && !isLocalhostDomain
        cookie.setAttribute("SameSite", if (isLocalhostDomain) "Lax" else "Strict")

        cookie.maxAge = when {
            cookieValue.isBlank() -> 0
            name == "accessToken" -> accessTokenExpiration.toInt()
            name == "refreshToken" -> refreshTokenExpiration.toInt()
            else -> -1
        }

        response.addCookie(cookie)
    }

    fun deleteCookie(name: String) {
        setCookie(name, null)
    }

    fun sendRedirect(url: String) {
        response.sendRedirect(url)
    }
}
