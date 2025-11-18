package com.back.motionit.domain.challenge.comment.entity

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.user.entity.User
import com.back.motionit.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "room_comments",
    indexes = [
        Index(
            name = "idx_room_comments_room_created",
            columnList = "room_id, create_date DESC"
        ),
        Index(
            name = "idx_room_comments_author",
            columnList = "author_id"
        )
    ]
)
class Comment(

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    var challengeRoom: ChallengeRoom,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    var user: User,

    @Column(nullable = false, length = 1000)
    var content: String,

    @Column(
        name = "like_count",
        nullable = false,
        columnDefinition = "INT DEFAULT 0"
    )
    var likeCount: Int = 0,

    // ++ Optimistic Lock
    @Version
    @Column(nullable = false)
    var version: Long? = null,
) : BaseEntity() {

    fun edit(newContent: String) {
        this.content = newContent
    }

    fun softDelete() {
        this.deletedAt = LocalDateTime.now()
    }

    val isDeleted: Boolean
        get() = deletedAt != null

    fun restore() {
        this.deletedAt = null
    }

    // ++ Like
    fun incrementLikeCount() {
        this.likeCount++
    }

    fun decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--
        }
    }
}
