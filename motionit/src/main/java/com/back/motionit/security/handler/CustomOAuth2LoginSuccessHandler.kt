package com.back.motionit.security.handler

import com.back.motionit.domain.auth.social.service.SocialAuthService
import com.back.motionit.global.request.RequestContext
import com.back.motionit.security.SecurityUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class CustomOAuth2LoginSuccessHandler(
    private val socialAuthService: SocialAuthService,
    private val requestContext: RequestContext
) : AuthenticationSuccessHandler {

    @Throws(IOException::class)
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val securityUser = authentication.principal as SecurityUser
        val userId = securityUser.id

        val tokens = socialAuthService.generateTokensById(userId)

        requestContext.setCookie("accessToken", tokens.accessToken)
        requestContext.setCookie("refreshToken", tokens.refreshToken)

        val state = request.getParameter("state")
        var redirectUrl = "/"

        if (!state.isNullOrBlank()) {
            val decodedState = String(
                Base64.getUrlDecoder().decode(state),
                StandardCharsets.UTF_8
            )
            redirectUrl = decodedState.split("#")[1]
        }

        requestContext.sendRedirect(redirectUrl)
    }
}
