package com.back.motionit.global.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.openai.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class GptService {

	private final OpenAiService openAiService;

	/**
	 * 미션 완료 시 GPT가 격려 메시지를 생성합니다.
	 * @param userName 사용자 이름
	 * @param challengeName 챌린지 이름
	 * @return GPT가 생성한 격려 메시지
	 */
	public String generateMissionCompleteSummary(String userName, String challengeName) {
		try {
			List<ChatMessage> messages = new ArrayList<>();

			// 시스템 프롬프트: GPT의 역할 정의
			messages.add(new ChatMessage(
				ChatMessageRole.SYSTEM.value(),
				"당신은 운동 챌린지 앱의 친근한 코치입니다. "
					+ "사용자가 미션을 완료했을 때 짧고 따뜻한 격려 메시지를 생성합니다. "
					+ "메시지는 2-3문장 이내로 간결하게 작성하고, 긍정적이고 동기부여가 되는 톤을 유지합니다."
			));

			// 사용자 프롬프트: 구체적인 요청
			messages.add(new ChatMessage(
				ChatMessageRole.USER.value(),
				String.format(
					"사용자 '%s'님이 '%s' 챌린지의 오늘 미션을 완료했습니다. "
						+ "축하와 격려의 메시지를 생성해주세요.",
					userName,
					challengeName
				)
			));

			ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
				.model("gpt-3.5-turbo")
				.messages(messages)
				.temperature(0.7)
				.maxTokens(150)
				.build();

			String response = openAiService.createChatCompletion(completionRequest)
				.getChoices()
				.get(0)
				.getMessage()
				.getContent();

			log.info("[GPT] Mission complete summary generated for user: {}", userName);
			return response;

		} catch (Exception e) {
			log.error("[GPT] Failed to generate mission summary for user: {}, challenge: {}", userName, challengeName,
				e);
			throw e;
		}
	}
}
