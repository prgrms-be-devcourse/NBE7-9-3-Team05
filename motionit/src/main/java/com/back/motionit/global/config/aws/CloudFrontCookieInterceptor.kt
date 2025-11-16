package com.back.motionit.global.config.aws

import com.back.motionit.global.service.CloudFrontCookieService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration

@Component
@ConditionalOnBean(CloudFrontCookieService::class)
class CloudFrontCookieInterceptor(
    private val cookieService: CloudFrontCookieService,

    @Value("\${aws.cloudfront.renew-seconds}")
    private val renewSeconds: String,

    @Value("\${aws.cloudfront.cookie-duration}")
    private val cookieDuration: String,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (!"GET".equals(request.method, ignoreCase = true)) {
            return true
        }

        if (!needsCookie(request)) {
            return true
        }

        cookieService.setSignedCookies(response, Duration.ofHours(cookieDuration.toInt().toLong()))
        return true
    }

    private fun needsCookie(request: HttpServletRequest): Boolean {
        val cookies = request.cookies ?: return true

        var expires: String? = null
        var keypair: String? = null

        for (cookie in cookies) {
            if ("CloudFront-Expires".equals(cookie.name, ignoreCase = true)) {
                expires = cookie.value
            }

            if ("CloudFront-Key-Pair-Id".equals(cookie.name, ignoreCase = true)) {
                keypair = cookie.value
            }
        }

        if (expires == null || keypair == null) {
            return true
        }

        try {
            val exp = expires.toLong()
            val now = System.currentTimeMillis()
            return (exp - now) <= renewSeconds!!.toLong()
        } catch (e: NumberFormatException) {
            return true
        }
    }
}
