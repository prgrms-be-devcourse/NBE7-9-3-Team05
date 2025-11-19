package com.back.motionit.domain.challenge.comment.repository

import com.back.motionit.domain.challenge.comment.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface CommentRepository :
    JpaRepository<Comment, Long>,
    CommentQueryRepository {

    @Query(
        """
        select c
        from Comment c
        where c.challengeRoom.id = :roomId
          and c.deletedAt is null
        order by c.createDate desc
        """
    )
    fun findActiveByRoomId(
        @Param("roomId") roomId: Long,
        pageable: Pageable,
    ): Page<Comment>

    @EntityGraph(attributePaths = ["user"])
    @Query(
        """
        select c
        from Comment c
        where c.challengeRoom.id = :roomId
          and c.deletedAt is null
        order by c.createDate desc
        """
    )
    fun findActiveByRoomIdWithAuthor(
        @Param("roomId") roomId: Long,
        pageable: Pageable,
    ): Page<Comment>

    fun findByIdAndChallengeRoom_Id(
        commentId: Long,
        roomId: Long,
    ): Optional<Comment>

    @EntityGraph(attributePaths = ["user"])
    fun findWithUserById(id: Long): Optional<Comment>

    fun findByIdAndChallengeRoom_IdAndDeletedAtIsNull(
        commentId: Long,
        roomId: Long,
    ): Optional<Comment>
}
