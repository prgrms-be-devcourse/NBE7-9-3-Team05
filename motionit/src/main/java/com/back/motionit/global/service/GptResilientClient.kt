package com.back.motionit.global.service

import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * OpenAI API 호출에 Resilience4j 패턴을 적용하는 클라이언트
 * - 서킷브레이커: API 장애 시 빠른 실패
 * - 레이트리미터: 비용 관리 (초당 5개 요청 제한)
 * - 재시도: 일시적 실패 대응
 * - Fallback: 기본 격려 메시지 제공
 */
@Component
@ConditionalOnProperty(name = ["app.openai.enabled"], havingValue = "true", matchIfMissing = true)
class GptResilientClient(
    private val openAiService: OpenAiService
) {
    private val log = KotlinLogging.logger {}

    @RateLimiter(name = "gptService")
    @Retry(name = "gptService")
    @CircuitBreaker(name = "gptService", fallbackMethod = "generateMessageFallback")
    fun generateEncouragementMessage(userName: String, challengeName: String): String {
        // 토큰 최적화: 간결한 프롬프트 사용
        val messages = listOf(
            ChatMessage(
                ChatMessageRole.SYSTEM.value(),
                "당신은 운동 코치입니다. 2-3문장으로 따뜻한 축하 메시지를 작성하세요."
            ),
            ChatMessage(
                ChatMessageRole.USER.value(),
                "$userName 님이 $challengeName 미션을 완료했습니다."
            )
        )

        val request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(messages)
            .temperature(0.7)
            .maxTokens(100)  // 150 → 100 (토큰 최적화)
            .build()

        val response = openAiService
            .createChatCompletion(request)
            .choices[0]
            .message
            .content

        log.info { "[GPT] 격려 메시지 생성 완료: user=$userName, challenge=$challengeName" }
        return response
    }

    /**
     * OpenAI API 호출 실패 시 fallback 메서드
     * 서킷이 OPEN되거나 모든 재시도가 실패하면 호출됨
     */
    private fun generateMessageFallback(userName: String, challengeName: String, ex: Exception): String {
        log.warn(ex) { "[GPT] API 호출 실패, fallback 메시지 반환: user=$userName, challenge=$challengeName" }

        return """
            축하합니다, $userName 님! 🎉
            $challengeName 챌린지의 오늘 미션을 완료하셨네요!
            꾸준한 노력이 멋진 결과를 만들어냅니다. 내일도 화이팅! 💪
        """.trimIndent()
    }
}
