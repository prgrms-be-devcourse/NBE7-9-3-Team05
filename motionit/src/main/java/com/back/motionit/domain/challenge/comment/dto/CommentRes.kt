package com.back.motionit.domain.challenge.comment.dto

import com.back.motionit.domain.challenge.comment.entity.Comment
import java.time.LocalDateTime

data class CommentRes(
    val id: Long,
    val roomId: Long,
    val authorId: Long,
    val authorNickname: String,
    val content: String,
    val deleted: Boolean,
    val likeCount: Int,
    val liked: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        @JvmStatic
        fun from(comment: Comment, isLiked: Boolean): CommentRes {
            return CommentRes(
                id = comment.id!!,
                roomId = comment.challengeRoom.id!!,
                authorId = comment.user.id!!,
                authorNickname = comment.user.nickname,
                content = if (comment.isDeleted) "삭제된 댓글입니다" else comment.content,
                deleted = comment.isDeleted,
                likeCount = comment.likeCount,
                liked = isLiked,
                createdAt = comment.createDate!!,
                updatedAt = comment.modifyDate!!,
            )
        }
    }
}