package com.back.motionit.global.service

import com.theokanning.openai.completion.chat.ChatCompletionChoice
import com.theokanning.openai.completion.chat.ChatCompletionResult
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any

@ExtendWith(MockitoExtension::class)
@DisplayName("GptResilientClient 단위 테스트")
class GptResilientClientTest {

    @InjectMocks
    private lateinit var gptResilientClient: GptResilientClient

    @Mock
    private lateinit var openAiService: OpenAiService

    @Test
    @DisplayName("응원 메시지 생성 - 성공")
    fun generateEncouragementMessage_success() {
        // given
        val userName = "홍길동"
        val challengeName = "아침 운동 챌린지"
        val expectedMessage = "축하합니다, 홍길동 님! 🎉\n아침 운동 챌린지를 완료하셨네요!"

        val chatMessage = mock(ChatMessage::class.java)
        given(chatMessage.content).willReturn(expectedMessage)

        val choice = mock(ChatCompletionChoice::class.java)
        given(choice.message).willReturn(chatMessage)

        val result = mock(ChatCompletionResult::class.java)
        given(result.choices).willReturn(listOf(choice))

        given(openAiService.createChatCompletion(any()))
            .willReturn(result)

        // when
        val response = gptResilientClient.generateEncouragementMessage(userName, challengeName)

        // then
        assertNotNull(response)
        assertEquals(expectedMessage, response)
        verify(openAiService).createChatCompletion(any())
    }

    @Test
    @DisplayName("응원 메시지 생성 - 다양한 사용자 테스트")
    fun generateEncouragementMessage_variousUsers() {
        // given
        val testCases = listOf(
            "김철수" to "매일 걷기",
            "이영희" to "홈트레이닝",
            "박민수" to "저녁 조깅"
        )

        testCases.forEach { (userName, challengeName) ->
            val message = "$userName 님, $challengeName 미션 완료!"

            val chatMessage = mock(ChatMessage::class.java)
            given(chatMessage.content).willReturn(message)

            val choice = mock(ChatCompletionChoice::class.java)
            given(choice.message).willReturn(chatMessage)

            val result = mock(ChatCompletionResult::class.java)
            given(result.choices).willReturn(listOf(choice))

            given(openAiService.createChatCompletion(any()))
                .willReturn(result)

            // when
            val response = gptResilientClient.generateEncouragementMessage(userName, challengeName)

            // then
            assertEquals(message, response)
        }
    }

    @Test
    @DisplayName("응원 메시지 생성 - 빈 메시지도 처리")
    fun generateEncouragementMessage_emptyMessage() {
        // given
        val userName = "테스터"
        val challengeName = "테스트 챌린지"
        val emptyMessage = ""

        val chatMessage = mock(ChatMessage::class.java)
        given(chatMessage.content).willReturn(emptyMessage)

        val choice = mock(ChatCompletionChoice::class.java)
        given(choice.message).willReturn(chatMessage)

        val result = mock(ChatCompletionResult::class.java)
        given(result.choices).willReturn(listOf(choice))

        given(openAiService.createChatCompletion(any()))
            .willReturn(result)

        // when
        val response = gptResilientClient.generateEncouragementMessage(userName, challengeName)

        // then
        assertNotNull(response)
        assertEquals("", response)
    }

    @Test
    @DisplayName("응원 메시지 생성 - 긴 메시지도 처리")
    fun generateEncouragementMessage_longMessage() {
        // given
        val userName = "테스터"
        val challengeName = "마라톤 챌린지"
        val longMessage = """
            축하합니다, 테스터 님! 🎉
            마라톤 챌린지의 오늘 미션을 완료하셨네요!
            매일 꾸준히 노력하시는 모습이 정말 대단합니다.
            이런 노력이 쌓여서 멋진 결과를 만들어낼 것입니다.
            내일도 화이팅하세요! 💪
        """.trimIndent()

        val chatMessage = mock(ChatMessage::class.java)
        given(chatMessage.content).willReturn(longMessage)

        val choice = mock(ChatCompletionChoice::class.java)
        given(choice.message).willReturn(chatMessage)

        val result = mock(ChatCompletionResult::class.java)
        given(result.choices).willReturn(listOf(choice))

        given(openAiService.createChatCompletion(any()))
            .willReturn(result)

        // when
        val response = gptResilientClient.generateEncouragementMessage(userName, challengeName)

        // then
        assertNotNull(response)
        assertTrue(response.length > 50)
        assertEquals(longMessage, response)
    }

    @Test
    @DisplayName("응원 메시지 생성 - 특수문자 포함 테스트")
    fun generateEncouragementMessage_specialCharacters() {
        // given
        val userName = "테스터🏃"
        val challengeName = "운동💪챌린지"
        val message = "축하합니다! ${userName} 님이 ${challengeName}를 완료하셨어요! 🎉"

        val chatMessage = mock(ChatMessage::class.java)
        given(chatMessage.content).willReturn(message)

        val choice = mock(ChatCompletionChoice::class.java)
        given(choice.message).willReturn(chatMessage)

        val result = mock(ChatCompletionResult::class.java)
        given(result.choices).willReturn(listOf(choice))

        given(openAiService.createChatCompletion(any()))
            .willReturn(result)

        // when
        val response = gptResilientClient.generateEncouragementMessage(userName, challengeName)

        // then
        assertNotNull(response)
        assertTrue(response.contains("🎉"))
        assertEquals(message, response)
    }
}
