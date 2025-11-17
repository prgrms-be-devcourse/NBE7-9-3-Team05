package com.back.motionit.domain.challenge.like.repository

import com.back.motionit.domain.challenge.comment.entity.Comment
import com.back.motionit.domain.challenge.like.entity.CommentLike
import com.back.motionit.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentLikeRepository : JpaRepository<CommentLike, Long> {
    fun deleteByCommentAndUser(comment: Comment, user: User)
    fun existsByCommentAndUser(comment: Comment, user: User): Boolean
    fun deleteAllByComment(comment: Comment)

    @Query("""
        select cl.comment.id
        from CommentLike cl
        where cl.user = :user
          and cl.comment.id in :commentIds
        """)
    fun findCommentIdsLikedByUserInCommentList(
        @Param("user") user: User,
        @Param("commentIds") commentIds: List<Long>
    ): Set<Long>

}
