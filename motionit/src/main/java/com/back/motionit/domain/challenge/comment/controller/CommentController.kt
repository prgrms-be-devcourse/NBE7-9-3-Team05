package com.back.motionit.domain.challenge.comment.controller

import com.back.motionit.domain.challenge.comment.dto.CommentCreateReq
import com.back.motionit.domain.challenge.comment.dto.CommentEditReq
import com.back.motionit.domain.challenge.comment.dto.CommentRes
import com.back.motionit.domain.challenge.comment.service.CommentService
import com.back.motionit.global.request.RequestContext
import com.back.motionit.global.respoonsedata.ResponseData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*

@Tag(name = "Comment API", description = "운동방 댓글 CRUD 기능")
@RestController
@RequestMapping("/api/v1/rooms/{roomId}/comments")
class CommentController(
    private val commentService: CommentService,
    private val requestContext: RequestContext,
) {

    @Operation(summary = "댓글 생성", description = "운동방에 댓글을 작성합니다.")
    @PostMapping
    fun create(
        @PathVariable("roomId") roomId: Long,
        @Valid @RequestBody req: CommentCreateReq,
    ): ResponseData<CommentRes> {
        val actor = requestContext.actor
        val userId = requireNotNull(actor.id) { "Authenticated user id is null" }

        val res = commentService.create(roomId, userId, req)
        return ResponseData.success("M-201", "created", res)
    }

    @Operation(summary = "댓글 목록 조회", description = "운동방 댓글을 페이지 단위로 조회합니다.")
    @GetMapping
    fun list(
        @PathVariable("roomId") roomId: Long,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
    ): ResponseData<Page<CommentRes>> {
        val actor = requestContext.actor
        val userId = requireNotNull(actor.id) { "Authenticated user id is null" }

        val resPage = commentService.list(roomId, userId, page, size)
        return ResponseData.success("M-200", "success", resPage)
    }

    @Operation(summary = "댓글 수정", description = "작성자가 본인의 댓글을 수정합니다.")
    @PatchMapping("/{commentId}")
    fun edit(
        @PathVariable("roomId") roomId: Long,
        @PathVariable("commentId") commentId: Long,
        @Valid @RequestBody req: CommentEditReq,
    ): ResponseData<CommentRes> {
        val actor = requestContext.actor
        val userId = requireNotNull(actor.id) { "Authenticated user id is null" }

        val res = commentService.edit(roomId, commentId, userId, req)
        return ResponseData.success("M-200", "updated", res)
    }

    @Operation(summary = "댓글 삭제", description = "작성자가 본인의 댓글을 삭제합니다.")
    @DeleteMapping("/{commentId}")
    fun delete(
        @PathVariable("roomId") roomId: Long,
        @PathVariable("commentId") commentId: Long,
    ): ResponseData<CommentRes> {
        val actor = requestContext.actor
        val userId = requireNotNull(actor.id) { "Authenticated user id is null" }

        val deletedComment = commentService.delete(roomId, commentId, userId)
        return ResponseData.success("M-200", "deleted", deletedComment)
    }

    private fun extractUserId(authUser: Any?): Long = 1L
}
