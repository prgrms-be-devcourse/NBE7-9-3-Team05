package com.back.motionit.domain.challenge.comment.repository

import com.back.motionit.domain.challenge.comment.dto.CommentRes
import com.back.motionit.domain.challenge.comment.entity.QComment.comment
import com.back.motionit.domain.challenge.like.entity.QCommentLike.commentLike
import com.back.motionit.domain.user.entity.QUser.user
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
open class CommentQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : CommentQueryRepository {

    override fun findCommentsWithAuthorAndLike(
        roomId: Long,
        userId: Long,
        pageable: Pageable,
    ): Page<CommentRes> {
        val basePredicate = comment.challengeRoom.id.eq(roomId)
            .and(comment.deletedAt.isNull)

        val contents: List<CommentRes> = queryFactory
            .select(
                Projections.constructor(
                    CommentRes::class.java,
                    comment.id,
                    comment.challengeRoom.id,
                    user.id,
                    user.nickname,
                    comment.content,
                    comment.deletedAt.isNotNull,
                    comment.likeCount,
                    commentLike.id.isNotNull,
                    comment.createDate,
                    comment.modifyDate,
                )
            )
            .from(comment)
            .join(comment.user, user)
            .leftJoin(commentLike)
            .on(
                commentLike.comment.eq(comment),
                commentLike.user.id.eq(userId)
            )
            .where(basePredicate)
            .orderBy(comment.createDate.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        if (contents.isEmpty()) {
            return PageImpl(emptyList(), pageable, 0)
        }

        val total: Long = queryFactory
            .select(comment.id.count())
            .from(comment)
            .where(basePredicate)
            .fetchOne() ?: 0L

        return PageImpl(contents, pageable, total)
    }
}