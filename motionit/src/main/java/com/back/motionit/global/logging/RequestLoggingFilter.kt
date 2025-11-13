package com.back.motionit.global.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.*

@Component
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = KotlinLogging.logger {}

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val traceId = UUID.randomUUID().toString().substring(0, 8)

        MDC.put("traceId", traceId)
        MDC.put("uri", request.requestURI)
        MDC.put("method", request.method)

        val start = System.currentTimeMillis()

        try {
            filterChain.doFilter(request, response)
        } finally {
            val durationMs = System.currentTimeMillis() - start
            // 지연(lazy) 로깅, info 레벨이 켜져있을 때만 작동
            log.info{
                "[Request] method=${request.method} uri=${request.requestURI} " +
                        "status=${response.status} duration=${durationMs}ms traceId=$traceId"
            }

            MDC.clear()
        }
    }
}