package com.back.motionit.global.service

import com.amazonaws.services.cloudfront.CloudFrontCookieSigner
import com.amazonaws.services.cloudfront.util.SignerUtils
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import java.security.PrivateKey
import java.time.Duration
import java.time.Instant
import java.util.*

@Component
@ConditionalOnProperty(name = ["app.aws.enabled"], havingValue = "true", matchIfMissing = true)
@Profile("!test")
class CloudFrontCookieService(
    @Value("\${aws.cloudfront.domain}")
    private val cloudFrontDomain: String,

    @Value("\${aws.cloudfront.key-id}")
    private val keyPairId: String,

    @Value("\${aws.cloudfront.private-key-path}")
    private val privateKeyPath: String,
) {

    private var privateKey: PrivateKey? = null
    @PostConstruct
    fun init() {
        val keyFile = ResourceUtils.getFile(privateKeyPath)
        this.privateKey = SignerUtils.loadPrivateKey(keyFile)
    }

    fun setSignedCookies(response: HttpServletResponse, ttl: Duration?) {
        val expiresOn = Date.from(Instant.now().plus(ttl))
        val resourcePattern = "https://$cloudFrontDomain/*"

        val cookies =
            CloudFrontCookieSigner.getCookiesForCannedPolicy(
                SignerUtils.Protocol.https,
                cloudFrontDomain,
                privateKey,
                resourcePattern,
                keyPairId,
                expiresOn
            )

        add(response, cookies.keyPairId, true)
        add(response, cookies.signature, true)
        add(response, cookies.getExpires(), true)
    }

    private fun add(response: HttpServletResponse, entry: Map.Entry<String, String>, httpOnly: Boolean) {
        val cookie = Cookie(entry.key, entry.value)
        cookie.path = "/"
        cookie.secure = true
        cookie.isHttpOnly = httpOnly
        response.addCookie(cookie)
    }
}
