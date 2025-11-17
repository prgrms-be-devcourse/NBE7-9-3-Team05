package com.back.motionit.domain.challenge.like.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.motionit.domain.challenge.comment.dto.CommentRes;
import com.back.motionit.domain.challenge.comment.entity.Comment;
import com.back.motionit.domain.challenge.comment.repository.CommentRepository;
import com.back.motionit.domain.challenge.like.entity.CommentLike;
import com.back.motionit.domain.challenge.like.repository.CommentLikeRepository;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.error.code.CommentLikeErrorCode;
import com.back.motionit.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentLikeService {

	private final CommentLikeRepository commentLikeRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;

	/**
	 * @param commentId 댓글 ID
	 * @param userId    사용자 ID
	 * @return CommentRes 업데이트된 댓글 DTO
	 */
	public CommentRes toggleCommentLikeByCommentId(Long commentId, Long userId) {
		Comment comment = commentRepository.findById(commentId)
			.orElseThrow(() -> new BusinessException(CommentLikeErrorCode.COMMENT_NOT_FOUND));

		User user = userRepository.getReferenceById(userId);

		try {
			boolean isLiked;

			if (commentLikeRepository.existsByCommentAndUser(comment, user)) {
				cancelLike(comment, user);
				isLiked = false;
			} else {
				createLike(comment, user);
				isLiked = true;
			}

			return CommentRes.from(comment, isLiked);

		} catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException e) {
			throw new BusinessException(CommentLikeErrorCode.LIKE_TOGGLE_FAILED);
		}
	}

	private void createLike(Comment comment, User user) {
		CommentLike commentLike = CommentLike.builder()
			.comment(comment)
			.user(user)
			.build();
		commentLikeRepository.save(commentLike);
		comment.incrementLikeCount();
	}

	private void cancelLike(Comment comment, User user) {
		commentLikeRepository.findByCommentAndUser(comment, user)
			.ifPresent(commentLike -> {
				commentLikeRepository.delete(commentLike);
				comment.decrementLikeCount();
			});
	}
}
