package com.back.motionit.domain.challenge.video

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class YoutubeKeyTest {

    @Value("\${youtube.api.key:}")
    lateinit var youtubeApiKey: String   // lateinit + default blank

    @Test
    fun `youtube API key should be loaded`() {
        println("Loaded key: '$youtubeApiKey'")

        // 값이 비어있으면 어떤 문제가 있는지 친절하게 메시지 제공
        assertThat(youtubeApiKey)
            .withFailMessage("""
                youtube.api.key가 로드되지 않았습니다.
                - test 프로필에서 정상적으로 설정되어 있는지 확인하세요.
                - .env.properties 또는 GitHub Actions 환경변수 확인 필요.
            """.trimIndent())
            .isNotBlank()
    }
}