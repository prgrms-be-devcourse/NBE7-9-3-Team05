package com.back.motionit.domain.challenge.comment.moderation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class KeywordFilterTest {

    // ---------------- BLOCK ----------------

    @Test
    @DisplayName("BLOCK: 강한 욕설은 차단된다(씨발/시발/18/개새끼 등)")
    fun block_basic() {
        // 점/공백 제거 후 '씨발' 매칭
        assertThat(KeywordFilter.decide("스쿼트 그렇게 하는 거 아니라고 씨 바..."))
            .isEqualTo(KeywordFilter.Decision.BLOCK)

        // 시발 계열은 BLOCK 리스트에 포함되어야 함
        assertThat(KeywordFilter.decide("시발 뭐함?"))
            .isEqualTo(KeywordFilter.Decision.BLOCK)
        assertThat(KeywordFilter.decide("시.발 같은 소리"))
            .isEqualTo(KeywordFilter.Decision.BLOCK)
        assertThat(KeywordFilter.decide("시바라 진짜"))
            .isEqualTo(KeywordFilter.Decision.BLOCK)
        assertThat(KeywordFilter.decide("씨1바 그만해"))
            .isEqualTo(KeywordFilter.Decision.BLOCK)

        // 숫자형 욕설
        assertThat(KeywordFilter.decide("개 못해 18놈아"))
            .isEqualTo(KeywordFilter.Decision.BLOCK)

        assertThat(KeywordFilter.decide("그따구로 할거면 때려쳐 등 신아"))
            .isEqualTo(KeywordFilter.Decision.BLOCK)
        assertThat(KeywordFilter.decide("mother_fucker"))
            .isEqualTo(KeywordFilter.Decision.BLOCK)
    }

    @Test
    @DisplayName("BLOCK: 화이트리스트 마스킹 후 남은 욕설은 차단")
    fun block_after_whitelist_mask() {
        // '병신도'는 제거되지만 뒤의 '시발'은 BLOCK
        assertThat(KeywordFilter.decide("병신도부터 문제였어 시발"))
            .isEqualTo(KeywordFilter.Decision.BLOCK)

        assertThat(KeywordFilter.decide("보지못한게 자랑이냐 등신아?"))
            .isEqualTo(KeywordFilter.Decision.BLOCK)
    }

    // ---------------- WARN ----------------

    @Test
    @DisplayName("WARN: 축약/약한 욕설은 경고된다(돌았네, 정신병자, ㅈㄹㄴ 등)")
    fun warn_basic() {
        assertThat(KeywordFilter.decide("idiot"))
            .isEqualTo(KeywordFilter.Decision.WARN)

        assertThat(KeywordFilter.decide("운동 강도 진짜 돌았네"))
            .isEqualTo(KeywordFilter.Decision.WARN)

        assertThat(KeywordFilter.decide("정신병자세요?"))
            .isEqualTo(KeywordFilter.Decision.WARN)
    }

    // ---------------- ALLOW ----------------

    @Test
    @DisplayName("ALLOW: 화이트리스트 단어만 있을 때는 통과된다")
    fun whitelist_allow_only() {
        assertThat(KeywordFilter.decide("시발점 찾는 법 설명"))
            .isEqualTo(KeywordFilter.Decision.ALLOW)

        assertThat(KeywordFilter.decide("보지말다 자지러지다 보지않다 꺼져가다"))
            .isEqualTo(KeywordFilter.Decision.ALLOW)

        assertThat(KeywordFilter.decide("보지마 자지마"))
            .isEqualTo(KeywordFilter.Decision.ALLOW)
    }

    @Test
    @DisplayName("ALLOW: 화이트리스트가 포함되어도 욕설이 없으면 통과")
    fun whitelist_mixed_without_abuse() {
        assertThat(KeywordFilter.decide("오랜시간 자지 않아서 머리가 안 돌아가"))
            .isEqualTo(KeywordFilter.Decision.ALLOW)
    }

    // ---------------- COMBO ----------------

    @Test
    @DisplayName("COMBO: 화이트리스트 + 욕설 동시 포함 시 욕설이 우선한다")
    fun whitelist_plus_abuse() {
        assertThat(KeywordFilter.decide("자지러졌어 이 새끼야"))
            .isEqualTo(KeywordFilter.Decision.BLOCK)

        assertThat(KeywordFilter.decide("보지않았어 그래서 멍청이가 됐어"))
            .isEqualTo(KeywordFilter.Decision.WARN)
    }
}
