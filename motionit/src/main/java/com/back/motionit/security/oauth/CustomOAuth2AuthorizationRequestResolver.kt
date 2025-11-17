package com.back.motionit.security.oauth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
@ConditionalOnProperty(name = ["app.oauth2.enabled"], havingValue = "true", matchIfMissing = true)
class CustomOAuth2AuthorizationRequestResolver(
    private val clientRegistrationRepository: ClientRegistrationRepository
) : OAuth2AuthorizationRequestResolver {

    @Value("\${app.oauth2.redirect-url}")
    private lateinit var frontendRedirectUrl: String

    private fun defaultResolver(): DefaultOAuth2AuthorizationRequestResolver {
        return DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository,
            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
        )
    }

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        val req = defaultResolver().resolve(request)
        return req?.let { customizeState(it, request) }
    }

    override fun resolve(request: HttpServletRequest, clientRegistrationId: String): OAuth2AuthorizationRequest? {
        val req = defaultResolver().resolve(request, clientRegistrationId)
        return req?.let { customizeState(it, request) }
    }

    private fun customizeState(
        authorizationRequest: OAuth2AuthorizationRequest?,
        req: HttpServletRequest
    ): OAuth2AuthorizationRequest? {
        //OAuth 요청이 아닐 때는 넘어감
        if (authorizationRequest == null) {
            return null
        }

        var redirectUrl = req.getParameter("redirectUrl")

        if (redirectUrl.isNullOrBlank()) {
            redirectUrl = frontendRedirectUrl
        }

        val originState = authorizationRequest.state ?: ""

        val newState = "$originState#$redirectUrl"

        val encodedNewState = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(newState.toByteArray(StandardCharsets.UTF_8))

        return OAuth2AuthorizationRequest.from(authorizationRequest)
            .state(encodedNewState)
            .build()
    }
}
