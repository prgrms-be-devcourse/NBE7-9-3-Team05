package com.back.motionit.security.socket

import com.back.motionit.security.jwt.JwtTokenProvider
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component

@Component
class StompAuthChannelInterceptor(
    private val tokenProvider: JwtTokenProvider
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = StompHeaderAccessor.wrap(message)
        val command = accessor.command

        if (StompCommand.CONNECT == command) {
            val auth = accessor.getFirstNativeHeader("Authorization") ?: ""
            val userId = verifyJwtAndGetUserId(auth)

            if (userId != null) {
                val authToken = UsernamePasswordAuthenticationToken(userId, null, listOf())
                accessor.user = authToken
            }

            return MessageBuilder.createMessage(message.payload, accessor.messageHeaders)
        }

        return message
    }

    private fun verifyJwtAndGetUserId(authHeader: String): Long? {

        val token = authHeader
            .takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()
            ?: return null

        return runCatching {
            val payload = tokenProvider.payloadOrNull(token) ?: return null

            val userId = payload["id"] as? Number ?: return null
            userId.toLong()
        }.getOrNull()
    }
}
