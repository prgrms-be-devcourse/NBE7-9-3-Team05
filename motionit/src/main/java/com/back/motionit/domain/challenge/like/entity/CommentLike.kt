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
open class CommentLike() : BaseEntity() { // 기본 생성자 추가
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    open lateinit var comment: Comment

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    open lateinit var user: User

    constructor(comment: Comment, user: User) : this() {
        this.comment = comment
        this.user = user
    }

    companion object {
        fun create(comment: Comment, user: User) = CommentLike(comment, user)
    }
}
