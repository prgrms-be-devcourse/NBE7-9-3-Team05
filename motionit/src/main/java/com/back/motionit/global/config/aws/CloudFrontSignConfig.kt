package com.back.motionit.global.config.aws

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner
import com.amazonaws.services.cloudfront.util.SignerUtils
import com.back.motionit.global.error.code.ConfigErrorCode
import com.back.motionit.global.error.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.PrivateKey
import java.time.Instant
import java.util.*

@Configuration
@ConditionalOnProperty(name = ["app.aws.enabled"], havingValue = "true", matchIfMissing = true)
class CloudFrontSignConfig(
    @Value("\${aws.cloudfront.domain}")
    private val cloudFrontDomain: String,

    @Value("\${aws.cloudfront.key-id}")
    private val keyPairId: String,

    @Value("\${aws.cloudfront.private-key-path}")
    private val privateKeyPath: String,
) {

    @Bean
    fun cloudFrontSigner(): CloudFrontSigner =
        CloudFrontSigner(cloudFrontDomain, keyPairId, loadPrivateKey(privateKeyPath))

    private fun loadPrivateKey(location: String): PrivateKey {
        try {
            if (location.startsWith("classpath:")) {
                val cp = location.replace("classpath:", "")
                javaClass.getResourceAsStream(cp).use { `in` ->
                    checkNotNull(`in`) { "Classpath resource not found: $cp" }
                    val tmp = Files.createTempFile("cf-key-", ".pem")
                    Files.copy(`in`, tmp, StandardCopyOption.REPLACE_EXISTING)
                    try {
                        return SignerUtils.loadPrivateKey(tmp.toFile())
                    } finally {
                        Files.deleteIfExists(tmp)
                    }
                }
            } else {
                val path = location.replaceFirst("^file:".toRegex(), "")
                return SignerUtils.loadPrivateKey(File(path))
            }
        } catch (e: Exception) {
            throw BusinessException(ConfigErrorCode.FAILED_LOAD_PRIVATE_KEY)
        }
    }

    class CloudFrontSigner(
        private val domain: String,
        private val keyPairId: String,
        private val privateKey: PrivateKey
    ) {
        fun signUrl(objectKey: String, expiresAt: Instant): String {
            val resourceUrl = String.format("https://%s/%s", domain, objectKey)
            val expires = Date.from(expiresAt)
            try {
                return CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
                    resourceUrl, keyPairId, privateKey, expires
                )
            } catch (e: Exception) {
                throw BusinessException(ConfigErrorCode.FAILED_SIGN_CLOUD_FRONT_URL)
            }
        }
    }
}
