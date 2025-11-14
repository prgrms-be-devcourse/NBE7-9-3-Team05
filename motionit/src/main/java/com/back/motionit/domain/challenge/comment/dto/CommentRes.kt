package com.back.motionit.domain.challenge.comment.dto;

import java.time.LocalDateTime;

import com.back.motionit.domain.challenge.comment.entity.Comment;

public record CommentRes(
	Long id,
	Long roomId,
	Long authorId,
	String authorNickname,
	String content,
	boolean deleted,
	Integer likeCount,
	boolean isLiked,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static CommentRes from(Comment comment, boolean isLiked) {
		boolean deleted = comment.isDeleted();

		return new CommentRes(
			comment.getId(),
			comment.getChallengeRoom().getId(),
			comment.getUser().getId(),
			comment.getUser().getNickname(),
			comment.isDeleted() ? "삭제된 댓글입니다" : comment.getContent(),
			comment.isDeleted(),
			comment.getLikeCount(),
			isLiked,
			comment.getCreateDate(),
			comment.getModifyDate()
		);
	}
}
