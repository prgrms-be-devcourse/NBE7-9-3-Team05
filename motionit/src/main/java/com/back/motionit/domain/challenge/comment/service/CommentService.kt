package com.back.motionit.domain.challenge.comment.service

import com.back.motionit.domain.challenge.comment.dto.CommentCreateReq
import com.back.motionit.domain.challenge.comment.dto.CommentEditReq
import com.back.motionit.domain.challenge.comment.dto.CommentRes
import com.back.motionit.domain.challenge.comment.entity.Comment
import com.back.motionit.domain.challenge.comment.moderation.CommentModeration
import com.back.motionit.domain.challenge.comment.repository.CommentRepository
import com.back.motionit.domain.challenge.like.repository.CommentLikeRepository
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.challenge.validator.ChallengeAuthValidator
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.error.code.CommentErrorCode
import com.back.motionit.global.error.exception.BusinessException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val challengeRoomRepository: ChallengeRoomRepository,
    private val userRepository: UserRepository,
    private val commentLikeRepository: CommentLikeRepository,
    private val commentModeration: CommentModeration,
    private val challengeAuthValidator: ChallengeAuthValidator,
) {

    private fun assertActiveRoomOrThrow(roomId: Long) {
        if (!challengeRoomRepository.existsById(roomId)) {
            throw BusinessException(CommentErrorCode.ROOM_NOT_FOUND)
        }
    }

    private fun loadActiveCommentOrThrow(roomId: Long, commentId: Long): Comment {
        return commentRepository
            .findByIdAndChallengeRoom_IdAndDeletedAtIsNull(commentId, roomId)
            .orElseThrow { BusinessException(CommentErrorCode.COMMENT_NOT_FOUND) }
    }

    private fun assertOwnerOrThrow(comment: Comment, userId: Long) {
        if (comment.user.id != userId) {
            throw BusinessException(CommentErrorCode.WRONG_ACCESS)
        }
    }

    @Transactional
    fun create(roomId: Long, userId: Long, req: CommentCreateReq): CommentRes {
        assertActiveRoomOrThrow(roomId)
        challengeAuthValidator.validateActiveParticipant(userId, roomId)

        val room: ChallengeRoom = challengeRoomRepository.findById(roomId)
            .orElseThrow { BusinessException(CommentErrorCode.ROOM_NOT_FOUND) }
            ?: throw BusinessException(CommentErrorCode.ROOM_NOT_FOUND)

        val author: User = userRepository.findById(userId)
            .orElseThrow { BusinessException(CommentErrorCode.USER_NOT_FOUND) }
            ?: throw BusinessException(CommentErrorCode.USER_NOT_FOUND)

        commentModeration.assertClean(req.content)

        val comment = Comment(
            deletedAt = null,
            challengeRoom = room,
            user = author,
            content = req.content,
            likeCount = 0,
            version = null,
        )

        commentRepository.save(comment)

        return CommentRes.from(comment, false)
    }

    @Transactional(readOnly = true)
    fun list(roomId: Long, userId: Long, page: Int, size: Int): Page<CommentRes> {
        assertActiveRoomOrThrow(roomId)
        challengeAuthValidator.validateActiveParticipant(userId, roomId)

        val user: User = userRepository.findById(userId)
            .orElseThrow { BusinessException(CommentErrorCode.USER_NOT_FOUND) }
            ?: throw BusinessException(CommentErrorCode.USER_NOT_FOUND)

        val pageable: Pageable = PageRequest.of(page, size)
        val comments: Page<Comment> =
            commentRepository.findActiveByRoomIdWithAuthor(roomId, pageable)

        if (comments.isEmpty) {
            return comments.map { CommentRes.from(it, false) }
        }

        val commentIds: List<Long> = comments.content
            .map { it.id!! }   // 저장된 댓글이라면 id는 반드시 존재

        val likedCommentIds: Set<Long> =
            commentLikeRepository.findLikedCommentIdsSafely(user, commentIds)

        return comments.map { c ->
            val isLiked = likedCommentIds.contains(c.id)
            CommentRes.from(c, isLiked)
        }
    }

    @Transactional
    fun edit(roomId: Long, commentId: Long, userId: Long, req: CommentEditReq): CommentRes {
        assertActiveRoomOrThrow(roomId)
        challengeAuthValidator.validateActiveParticipant(userId, roomId)

        val comment = loadActiveCommentOrThrow(roomId, commentId)
        assertOwnerOrThrow(comment, userId)

        commentModeration.assertClean(req.content)
        comment.edit(req.content)

        val user = userRepository.getReferenceById(userId)
        val isLiked = commentLikeRepository.existsByCommentAndUser(comment, user)

        return CommentRes.from(comment, isLiked)
    }

    @Transactional
    fun delete(roomId: Long, commentId: Long, userId: Long): CommentRes {
        assertActiveRoomOrThrow(roomId)
        challengeAuthValidator.validateActiveParticipant(userId, roomId)

        val comment = loadActiveCommentOrThrow(roomId, commentId)
        assertOwnerOrThrow(comment, userId)

        comment.softDelete()
        commentLikeRepository.deleteAllByComment(comment)

        return CommentRes.from(comment, false)
    }
}
