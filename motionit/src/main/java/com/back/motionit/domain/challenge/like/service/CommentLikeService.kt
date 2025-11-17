package com.back.motionit.domain.challenge.like.service

import com.back.motionit.domain.challenge.comment.dto.CommentRes
import com.back.motionit.domain.challenge.comment.entity.Comment
import com.back.motionit.domain.challenge.comment.repository.CommentRepository
import com.back.motionit.domain.challenge.like.entity.CommentLike
import com.back.motionit.domain.challenge.like.repository.CommentLikeRepository
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.error.code.CommentLikeErrorCode
import com.back.motionit.global.error.code.CommonErrorCode
import com.back.motionit.global.error.exception.BusinessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentLikeService(
    private val commentLikeRepository: CommentLikeRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository
) {
    fun findLikedCommentIdsSafely(user: User, commentIds: List<Long>?): Set<Long> {
        if (commentIds.isNullOrEmpty()) return emptySet()

        return commentLikeRepository.findCommentIdsLikedByUserInCommentList(user, commentIds)
    }

    fun toggleCommentLikeByCommentId(commentId: Long, userId: Long): CommentRes {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { BusinessException(CommentLikeErrorCode.COMMENT_NOT_FOUND) }

        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(CommonErrorCode.NOT_FOUND) }

        try {
            val isLiked = if (commentLikeRepository.existsByCommentAndUser(comment, user)) {
                commentLikeRepository.deleteByCommentAndUser(comment, user)
                comment.decrementLikeCount()
                false
            } else {
                createLike(comment, user)
                true
            }
            return CommentRes.from(comment, isLiked)
        } catch (e: ObjectOptimisticLockingFailureException) {
            throw BusinessException(CommentLikeErrorCode.LIKE_TOGGLE_FAILED)
        } catch (e: DataIntegrityViolationException) {
            throw BusinessException(CommentLikeErrorCode.LIKE_TOGGLE_FAILED)
        }
    }

    private fun createLike(comment: Comment, user: User) {
        commentLikeRepository.save(CommentLike(comment, user))
        comment.incrementLikeCount()
    }

}
