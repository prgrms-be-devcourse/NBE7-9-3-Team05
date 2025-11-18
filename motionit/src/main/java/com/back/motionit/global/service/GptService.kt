package com.back.motionit.global.service

import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["app.openai.enabled"], havingValue = "true", matchIfMissing = true)
class GptService(
    private val openAiService: OpenAiService
) {

    private val log = KotlinLogging.logger {}

    /**
     * 미션 완료 시 GPT가 격려 메시지를 생성합니다.
     * @param userName 사용자 이름
     * @param challengeName 챌린지 이름
     * @return GPT가 생성한 격려 메시지
     */
    fun generateMissionCompleteSummary(userName: String, challengeName: String): String {
        return runCatching {
            val messages: MutableList<ChatMessage> = mutableListOf(
                ChatMessage(
                    ChatMessageRole.SYSTEM.value(),
                    """
                    당신은 운동 챌린지 앱의 친근한 코치입니다.
                    사용자가 미션을 완료했을 때 짧고 따뜻한 격려 메시지를 생성합니다.
                    메시지는 2-3문장 이내로 간결하게 작성하고, 긍정적이고 동기부여가 되는 톤을 유지합니다.
                    """.trimIndent()
                ),
                ChatMessage(
                    ChatMessageRole.USER.value(),
                    "사용자 '$userName'님이 '$challengeName' 챌린지의 오늘 미션을 완료했습니다. " +
                            "축하와 격려의 메시지를 생성해주세요."
                )
            )

            val completionRequest = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(messages)
                .temperature(0.7)
                .maxTokens(150)
                .build()

            val response = openAiService
                .createChatCompletion(completionRequest)
                .choices[0]
                .message
                .content

            log.info { "[GPT] Mission complete summary generated for user: $userName" }
            response
        }.getOrElse { e->
            log.error {"[GPT] Failed to generate mission summary for user: $userName, challenge: $challengeName, Error: ${e}"}
            throw e
        }
    }
}
