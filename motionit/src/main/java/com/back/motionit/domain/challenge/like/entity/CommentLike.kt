package com.back.motionit.domain.challenge.like.entity

import com.back.motionit.domain.challenge.comment.entity.Comment
import com.back.motionit.domain.user.entity.User
import com.back.motionit.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "comment_likes",
    uniqueConstraints = [
        UniqueConstraint(name = "unique_like", columnNames = ["comment_id", "user_id"])
    ]
)
open class CommentLike(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    var comment: Comment,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User
) : BaseEntity(){
    companion object {
        fun create(comment: Comment, user: User) = CommentLike(comment, user)
    }
}