package com.back.motionit.domain.challenge.comment.repository

import com.back.motionit.domain.challenge.comment.dto.CommentRes
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CommentQueryRepository {

    fun findCommentsWithAuthorAndLike(
        roomId: Long,
        userId: Long,
        pageable: Pageable,
    ): Page<CommentRes>
}