package com.back.motionit.domain.challenge.comment.moderation

import com.back.motionit.global.error.code.CommentErrorCode
import com.back.motionit.global.error.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class CommentModeration {

    fun assertClean(content: String) {
        when (KeywordFilter.decide(content)) {
            KeywordFilter.Decision.BLOCK ->
                throw BusinessException(CommentErrorCode.INAPPROPRIATE_CONTENT_BLOCK)

            KeywordFilter.Decision.WARN ->
                throw BusinessException(CommentErrorCode.INAPPROPRIATE_CONTENT_WARN)

            KeywordFilter.Decision.ALLOW -> {

            }
        }
    }
}
