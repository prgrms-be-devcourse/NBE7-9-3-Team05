package com.back.motionit.global.service

import com.back.motionit.global.config.aws.CloudFrontSignConfig.CloudFrontSigner
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
@ConditionalOnProperty(name = ["app.aws.enabled"], havingValue = "true", matchIfMissing = true)
class AwsCdnSignService(
    private val signer: CloudFrontSigner,

    @Value("\${aws.cloudfront.signed-url-minutes:10}")
    private val signedMinutes: Long = 0
) {

    fun sign(objectKey: String): String {
        val expires = Instant.now().plus(signedMinutes, ChronoUnit.MINUTES)
        return signer.signUrl(objectKey, expires)
    }
}
