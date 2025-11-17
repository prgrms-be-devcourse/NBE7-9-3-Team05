package com.back.motionit.security

import com.back.motionit.domain.auth.social.service.SocialAuthService
import com.back.motionit.global.error.code.AuthErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.global.request.RequestContext
import com.back.motionit.global.respoonsedata.ResponseData
import com.back.motionit.security.jwt.JwtTokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class CustomAuthenticationFilter(
    private val socialAuthService: SocialAuthService,
    private val requestContext: RequestContext,
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    companion object {
        private const val AUTH_PATH_PREFIX = "/api/v1/auth/"
        private const val BEARER_PREFIX = "Bearer "
    }

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger.debug("CustomAuthenticationFilter called")

        addCorsHeaders(response)

        try {
            authenticate(request, response, filterChain)
        } catch (e: BusinessException) {
            writeErrorResponse(response, e)
        }
    }

    @Throws(ServletException::class, IOException::class)
    private fun authenticate(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestUri = request.requestURI

        if ("OPTIONS".equals(request.method, ignoreCase = true)) {
            filterChain.doFilter(request, response)
            return
        }

        if (!requestUri.startsWith("/api/")) {
            filterChain.doFilter(request, response)
            return
        }

        if (requestUri.startsWith(AUTH_PATH_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        // Authorization 헤더 확인
        val headerAuthorization = requestContext.getHeader("Authorization", "")

        val accessToken =
            if (headerAuthorization.isNotBlank() && headerAuthorization.startsWith(BEARER_PREFIX)) {
                headerAuthorization.substring(BEARER_PREFIX.length)
            } else {
                // 헤더 없으면 쿠키에서 accessToken 조회
                requestContext.getCookieValue("accessToken", "")
            }

        // 만료 확인
        if (jwtTokenProvider.isExpired(accessToken)) {
            throw BusinessException(AuthErrorCode.TOKEN_EXPIRED)
        }

        val payload =
            socialAuthService.payloadOrNull(accessToken) ?: throw BusinessException(AuthErrorCode.TOKEN_INVALID)

        val userId = (payload["id"] as Number).toLong()
        val nickname = payload.getOrDefault("nickname", "") as String

        val securityUser = SecurityUser(
            userId,
            "",
            nickname,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        val authentication: Authentication = UsernamePasswordAuthenticationToken(
            securityUser,
            null,
            securityUser.authorities
        )

        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(request, response)
    }

    private fun addCorsHeaders(response: HttpServletResponse) {
        response.apply {
            setHeader("Access-Control-Allow-Origin", "http://localhost:3000")
            setHeader("Access-Control-Allow-Credentials", "true")
            setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS")
            setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
            setHeader("Access-Control-Max-Age", "3600")
        }
    }

    @Throws(IOException::class)
    private fun writeErrorResponse(
        response: HttpServletResponse,
        exception: BusinessException
    ) {
        val body = ResponseData.error<Void>(exception.errorCode)

        response.apply {
            status = exception.errorCode.status.value()
            contentType = "application/json;charset=UTF-8"
        }

        val json = """
        {
          "resultCode": "${body.resultCode}",
          "msg": "${body.msg}",
          "data": null
        }
    """.trimIndent()

        response.writer.write(json)
    }
}

