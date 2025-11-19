package com.back.motionit.global.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@DisplayName("GptService 단위 테스트")
class GptServiceTest {

    @InjectMocks
    private lateinit var gptService: GptService

    @Mock
    private lateinit var gptResilientClient: GptResilientClient

    @Test
    @DisplayName("미션 완료 응원 메시지 생성 - 성공")
    fun generateMissionCompleteSummary_success() {
        // given
        val userName = "홍길동"
        val challengeName = "아침 운동 챌린지"
        val expectedMessage = "축하합니다, 홍길동 님! 아침 운동 챌린지를 완료하셨네요!"

        given(gptResilientClient.generateEncouragementMessage(userName, challengeName))
            .willReturn(expectedMessage)

        // when
        val result = gptService.generateMissionCompleteSummary(userName, challengeName)

        // then
        assertNotNull(result)
        assertEquals(expectedMessage, result)
        verify(gptResilientClient).generateEncouragementMessage(userName, challengeName)
    }

    @Test
    @DisplayName("미션 완료 응원 메시지 생성 - 다양한 사용자명 테스트")
    fun generateMissionCompleteSummary_variousUserNames() {
        // given
        val userNames = listOf("김철수", "이영희", "박민수")
        val challengeName = "매일 걷기 챌린지"

        userNames.forEach { userName ->
            val expectedMessage = "$userName 님, 오늘도 멋지게 완료하셨어요!"
            given(gptResilientClient.generateEncouragementMessage(userName, challengeName))
                .willReturn(expectedMessage)

            // when
            val result = gptService.generateMissionCompleteSummary(userName, challengeName)

            // then
            assertEquals(expectedMessage, result)
        }

        verify(gptResilientClient, org.mockito.Mockito.times(3))
            .generateEncouragementMessage(org.mockito.kotlin.any(), org.mockito.kotlin.eq(challengeName))
    }

    @Test
    @DisplayName("미션 완료 응원 메시지 생성 - 다양한 챌린지명 테스트")
    fun generateMissionCompleteSummary_variousChallengeNames() {
        // given
        val userName = "테스터"
        val challengeNames = listOf("아침 운동", "저녁 산책", "홈트레이닝")

        challengeNames.forEach { challengeName ->
            val expectedMessage = "축하합니다! $challengeName 미션 완료!"
            given(gptResilientClient.generateEncouragementMessage(userName, challengeName))
                .willReturn(expectedMessage)

            // when
            val result = gptService.generateMissionCompleteSummary(userName, challengeName)

            // then
            assertEquals(expectedMessage, result)
        }

        verify(gptResilientClient, org.mockito.Mockito.times(3))
            .generateEncouragementMessage(org.mockito.kotlin.eq(userName), org.mockito.kotlin.any())
    }
}
