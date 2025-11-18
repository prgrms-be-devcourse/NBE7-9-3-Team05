package com.back.motionit.domain.challenge.comment.moderation

import com.back.motionit.global.error.code.CommentErrorCode
import com.back.motionit.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CommentModerationTest {

    private val moderation = CommentModeration()

    @Test
    @DisplayName("BLOCK이면 INAPPROPRIATE_CONTENT_BLOCK 예외")
    fun assertClean_block() {
        assertThatThrownBy {
            moderation.assertClean("병신")
        }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining(CommentErrorCode.INAPPROPRIATE_CONTENT_BLOCK.message)
        // 혹은 .getMessage() 쓰고 싶으면:
        // .hasMessageContaining(CommentErrorCode.INAPPROPRIATE_CONTENT_BLOCK.getMessage())
    }

    @Test
    @DisplayName("WARN이면 INAPPROPRIATE_CONTENT_WARN 예외")
    fun assertClean_warn() {
        assertThatThrownBy {
            moderation.assertClean("틀딱")
        }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining(CommentErrorCode.INAPPROPRIATE_CONTENT_WARN.message)
    }

    @Test
    @DisplayName("ALLOW면 예외 없음")
    fun assertClean_allow() {
        moderation.assertClean("시발점 찾는 법 안내") // 예외 안 나면 통과
    }
}
