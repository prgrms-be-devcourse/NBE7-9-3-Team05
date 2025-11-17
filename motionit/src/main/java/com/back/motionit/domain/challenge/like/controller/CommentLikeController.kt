package com.back.motionit.domain.challenge.like.controller

import com.back.motionit.domain.challenge.comment.dto.CommentRes
import com.back.motionit.domain.challenge.like.api.CommentLikeApi
import com.back.motionit.domain.challenge.like.service.CommentLikeService
import com.back.motionit.domain.user.entity.User
import com.back.motionit.global.request.RequestContext
import com.back.motionit.global.respoonsedata.ResponseData
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/comments")
class CommentLikeController(
    private val commentLikeService: CommentLikeService,
    private val requestContext: RequestContext
) : CommentLikeApi {
    override fun toggleCommentLikeByCommentId(@PathVariable commentId: Long): ResponseData<CommentRes> {
        val actor: User = requestContext.actor
        val updatedComment = commentLikeService.toggleCommentLikeByCommentId(commentId, actor.id!!)
        val responseMessage = if (updatedComment.liked) "좋아요 성공" else "좋아요 취소 성공"
        return ResponseData.success("200", responseMessage, updatedComment)
    }
}