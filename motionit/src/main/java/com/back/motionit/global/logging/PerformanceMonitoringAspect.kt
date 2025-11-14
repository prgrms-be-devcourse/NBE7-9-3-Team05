package com.back.motionit.global.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Aspect
@Component
class PerformanceMonitoringAspect(
    private val meterRegistry: MeterRegistry
) {

    // Timer 캐싱 (고정 metric key -> Timer 인스턴스 재사용)
    private val timerCache = ConcurrentHashMap<String, Timer>()
    private val log = KotlinLogging.logger {}

    @Around(
        """
        execution(* com.back.motionit.domain.challenge..controller..*(..)) ||
        execution(* com.back.motionit.domain.challenge..service..*(..)) ||
        execution(* com.back.motionit.domain.challenge..repository..*(..))
        execution(* com.back.motionit.domain.auth.local..controller..*(..)) ||
        execution(* com.back.motionit.domain.auth.local..service..*(..)) ||
        execution(* com.back.motionit.domain.user..repository..*(..))
        """
    )
    fun measureExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val start = System.nanoTime()
        var success = true

        try {
            return joinPoint.proceed()
        } catch (ex: Exception) {
            success = false // 실패 여부 기록
            throw ex
        } finally {
            val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)

            val signature = joinPoint.signature as MethodSignature
            val className = signature.declaringType.simpleName
            val methodName = signature.name
            val layer = classifyLayer(className)

            // Timer key (캐싱)
            val cacheKey = "$layer.$className.$methodName"

            val timer = timerCache.computeIfAbsent(cacheKey) {
                Timer.builder(METRIC_NAME)
                    .description("Method execution time (ms)")
                    .tags(
                        "layer", layer,
                        "class", className,
                        "method", methodName,
                    )
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry)
            }

            // 측정값 기록
            timer.record(durationMs, TimeUnit.MILLISECONDS)

            // 지연(lazy) 로깅, debug 레벨이 켜져있을 때만 작동
            log.debug {
                "[Perf] $layer -> $className.$methodName executed in ${durationMs}ms (success=$success)"
            }
        }
    }

    private fun classifyLayer(className: String): String =
        when {
            "controller" in className.lowercase() -> "controller"
            "service" in className.lowercase() -> "service"
            "repository" in className.lowercase() -> "repository"
            else -> "other"
        }

    companion object {
        private const val METRIC_NAME = "method.execution.time"
    }
}