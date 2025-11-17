package com.back.motionit.domain.challenge.like.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.motionit.domain.challenge.comment.entity.Comment;
import com.back.motionit.domain.challenge.like.entity.CommentLike;
import com.back.motionit.domain.user.entity.User;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
	Optional<CommentLike> findByCommentAndUser(Comment comment, User user);

	void deleteByCommentAndUser(Comment comment, User user);

	boolean existsByCommentAndUser(Comment comment, User user);

	void deleteAllByComment(Comment comment);

	@Query("""
		select cl.comment.id
		from CommentLike cl
		where cl.user = :user
			and cl.comment.id in :commentIds
		""")
	Set<Long> findCommentIdsLikedByUserInCommentList(
		@Param("user") User user,
		@Param("commentIds") List<Long> commentIds
	);

	default Set<Long> findLikedCommentIdsSafely(User user, List<Long> commentIds) {
		if (commentIds == null || commentIds.isEmpty()) {
			return Collections.emptySet();
		}
		return findCommentIdsLikedByUserInCommentList(user, commentIds);
	}

}

