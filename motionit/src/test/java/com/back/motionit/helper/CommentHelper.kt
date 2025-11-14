package com.back.motionit.helper

import com.back.motionit.domain.challenge.comment.entity.Comment
import com.back.motionit.domain.challenge.comment.repository.CommentRepository
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.user.entity.User
import org.springframework.stereotype.Component

@Component
class CommentHelper internal constructor(
    private val commentRepository: CommentRepository
) {
    fun createComment(user: User, room: ChallengeRoom, content: String): Comment {
        val comment = Comment.builder()
            .user(user)
            .challengeRoom(room)
            .content(content)
            .build()
        return commentRepository.save(comment)
    }
}
