package com.back.motionit.global.error.code

import org.springframework.http.HttpStatus

enum class CommentLikeErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "L-301", "댓글을 찾을 수 없습니다."),
    LIKE_TOGGLE_FAILED(HttpStatus.CONFLICT, "L-302", "좋아요 처리 중 동시성 충돌이 발생했습니다. 다시 시도해 주세요.")
}