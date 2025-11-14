package com.back.motionit.helper;

import org.springframework.stereotype.Component;

import com.back.motionit.domain.challenge.comment.entity.Comment;
import com.back.motionit.domain.challenge.comment.repository.CommentRepository;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.user.entity.User;

@Component
public class CommentHelper {

	private final CommentRepository commentRepository;

	CommentHelper(CommentRepository commentRepository) {
		this.commentRepository = commentRepository;
	}

	public Comment createComment(User user, ChallengeRoom room, String content) {
		Comment comment = Comment.builder()
			.user(user)
			.challengeRoom(room)
			.content(content)
			.build();
		return commentRepository.save(comment);
	}

	public void clearComments() {
		commentRepository.deleteAll();
	}
}
