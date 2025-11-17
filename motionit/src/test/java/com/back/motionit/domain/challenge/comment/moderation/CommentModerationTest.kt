package com.back.motionit.domain.challenge.comment.moderation;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.back.motionit.global.error.code.CommentErrorCode;
import com.back.motionit.global.error.exception.BusinessException;

class CommentModerationTest {

	CommentModeration moderation = new CommentModeration();

	@Test
	@DisplayName("BLOCK이면 INAPPROPRIATE_CONTENT_BLOCK 예외")
	void assertClean_block() {
		assertThatThrownBy(() -> moderation.assertClean("병신"))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(CommentErrorCode.INAPPROPRIATE_CONTENT_BLOCK.getMessage());
	}

	@Test
	@DisplayName("WARN이면 INAPPROPRIATE_CONTENT_WARN 예외")
	void assertClean_warn() {
		assertThatThrownBy(() -> moderation.assertClean("틀딱"))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(CommentErrorCode.INAPPROPRIATE_CONTENT_WARN.getMessage());
	}

	@Test
	@DisplayName("ALLOW면 예외 없음")
	void assertClean_allow() {
		moderation.assertClean("시발점 찾는 법 안내"); // 통과
	}

}
