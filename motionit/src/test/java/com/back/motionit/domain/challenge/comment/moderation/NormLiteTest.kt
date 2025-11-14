package com.back.motionit.domain.challenge.comment.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NormLiteTest {

	@Test
	@DisplayName("점/공백/기호 제거")
	void removePunctuationsAndSpaces() {
		assertThat(NormLite.normalize("씨...발 뭐하냐")).isEqualTo("씨발뭐하냐");
		assertThat(NormLite.normalize("시. 발  같은 소리")).isEqualTo("시발같은소리");
		assertThat(NormLite.normalize("개_새_ 끼")).isEqualTo("개새끼");
		assertThat(NormLite.normalize("ㅅ-ㅂ_ 이건 좀")).isEqualTo("ㅅㅂ이건좀");
		assertThat(NormLite.normalize("시1발아")).isEqualTo("시발아");
	}

	@Test
	@DisplayName("제로폭 문자 제거")
	void removeZeroWidth() {
		String withZw = "ㅅ\u200Bㅂ 이건\u200D 좀\uFEFF";
		assertThat(NormLite.normalize(withZw)).isEqualTo("ㅅㅂ이건좀");
	}

	@Test
	@DisplayName("과도한 반복 축소(4회 이상 → 2회)")
	void shrinkRepetitions() {
		assertThat(NormLite.normalize("ㅋㅋㅋㅋㅋㅋ")).isEqualTo("ㅋㅋ");
		assertThat(NormLite.normalize("와아아아아아아아아아아")).isEqualTo("와아아");
	}

	@Test
	@DisplayName("NFKC + 소문자화 동작")
	void nfkcAndLowercase() {
		// 전각 -> 반각, 대문자 -> 소문자
		assertThat(NormLite.normalize("ＳＩＢＡＬ")).isEqualTo("sibal");
		// 섞여 있어도 정상 동작해야 함
		assertThat(NormLite.normalize("Si.Bal")).isEqualTo("sibal");
	}
}
