package com.back.motionit.domain.challenge.comment.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.motionit.domain.challenge.comment.dto.CommentCreateReq;
import com.back.motionit.domain.challenge.comment.dto.CommentEditReq;
import com.back.motionit.domain.challenge.comment.dto.CommentRes;
import com.back.motionit.domain.challenge.comment.entity.Comment;
import com.back.motionit.domain.challenge.comment.moderation.CommentModeration;
import com.back.motionit.domain.challenge.comment.repository.CommentRepository;
import com.back.motionit.domain.challenge.like.repository.CommentLikeRepository;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.challenge.validator.ChallengeAuthValidator;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.error.code.CommentErrorCode;
import com.back.motionit.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository commentRepository;
	private final ChallengeRoomRepository challengeRoomRepository;
	private final UserRepository userRepository;
	private final CommentLikeRepository commentLikeRepository;
	private final CommentModeration commentModeration;
	private final ChallengeAuthValidator challengeAuthValidator;

	private void assertActiveRoomOrThrow(Long roomId) {
		if (!challengeRoomRepository.existsById(roomId)) {
			throw new BusinessException(CommentErrorCode.ROOM_NOT_FOUND);
		}
	}

	private Comment loadActiveCommentOrThrow(Long roomId, Long commentId) {
		return commentRepository
			.findByIdAndChallengeRoom_IdAndDeletedAtIsNull(commentId, roomId)
			.orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
	}

	private void assertOwnerOrThrow(Comment comment, Long userId) {
		if (!comment.getUser().getId().equals(userId)) {
			throw new BusinessException(CommentErrorCode.WRONG_ACCESS);
		}
	}

	@Transactional
	public CommentRes create(Long roomId, Long userId, CommentCreateReq req) {

		assertActiveRoomOrThrow(roomId);
		challengeAuthValidator.validateActiveParticipant(userId, roomId);
		ChallengeRoom room = challengeRoomRepository.findById(roomId)
			.orElseThrow(() -> new BusinessException(CommentErrorCode.ROOM_NOT_FOUND));
		User author = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(CommentErrorCode.USER_NOT_FOUND));

		commentModeration.assertClean(req.content());

		Comment comment = Comment.builder()
			.challengeRoom(room)
			.user(author)
			.content(req.content())
			.build();
		commentRepository.save(comment);

		return CommentRes.from(comment, false);
	}

	@Transactional(readOnly = true)
	public Page<CommentRes> list(Long roomId, Long userId, int page, int size) {

		assertActiveRoomOrThrow(roomId);
		challengeAuthValidator.validateActiveParticipant(userId, roomId);

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(CommentErrorCode.USER_NOT_FOUND));

		Pageable pageable = PageRequest.of(page, size);
		Page<Comment> comments = commentRepository
			.findActiveByRoomIdWithAuthor(roomId, pageable);

		if (comments.isEmpty()) {
			return comments.map(c -> CommentRes.from(c, false));
		}

		List<Long> commentIds = comments.getContent().stream()
			.map(Comment::getId)
			.toList();

		Set<Long> likedCommentIds = commentLikeRepository
			.findLikedCommentIdsSafely(user, commentIds);

		return comments.map(c -> {
			boolean isLiked = likedCommentIds.contains(c.getId());
			return CommentRes.from(c, isLiked);
		});
	}

	@Transactional
	public CommentRes edit(Long roomId, Long commentId, Long userId, CommentEditReq req) {

		assertActiveRoomOrThrow(roomId);
		challengeAuthValidator.validateActiveParticipant(userId, roomId);
		Comment comment = loadActiveCommentOrThrow(roomId, commentId);
		assertOwnerOrThrow(comment, userId);
		commentModeration.assertClean(req.content());
		comment.edit(req.content());

		User user = userRepository.getReferenceById(userId);
		boolean isLiked = commentLikeRepository.existsByCommentAndUser(comment, user);

		return CommentRes.from(comment, isLiked);
	}

	@Transactional
	public CommentRes delete(Long roomId, Long commentId, Long userId) {

		assertActiveRoomOrThrow(roomId);
		challengeAuthValidator.validateActiveParticipant(userId, roomId);
		Comment comment = loadActiveCommentOrThrow(roomId, commentId);
		assertOwnerOrThrow(comment, userId);
		comment.softDelete();
		commentLikeRepository.deleteAllByComment(comment);

		return CommentRes.from(comment, false);
	}

}
