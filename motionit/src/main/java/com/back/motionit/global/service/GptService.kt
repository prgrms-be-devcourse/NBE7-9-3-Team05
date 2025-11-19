package com.back.motionit.global.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

/**
 * GPT 기반 메시지 생성 서비스
 * 실제 API 호출은 GptResilientClient에 위임하여 Resilience 패턴 적용
 */
@Service
@ConditionalOnProperty(name = ["app.openai.enabled"], havingValue = "true", matchIfMissing = true)
class GptService(
    private val gptResilientClient: GptResilientClient
) {
    /**
     * 미션 완료 시 GPT가 격려 메시지를 생성합니다.
     * @param userName 사용자 이름
     * @param challengeName 챌린지 이름
     * @return GPT가 생성한 격려 메시지
     */
    fun generateMissionCompleteSummary(userName: String, challengeName: String): String {
        return gptResilientClient.generateEncouragementMessage(userName, challengeName)
    }
}
