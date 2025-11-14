package com.back.motionit.domain.challenge.comment.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.motionit.domain.challenge.comment.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@Query("""
			select c
			from Comment c
			where c.challengeRoom.id = :roomId
				and c.deletedAt is null
			order by c.createDate desc
		""")
	Page<Comment> findActiveByRoomId(@Param("roomId") Long roomId, Pageable pageable);

	@EntityGraph(attributePaths = "user")
	@Query("""
			select c
			from Comment c
			where c.challengeRoom.id = :roomId
				and c.deletedAt is null
			order by c.createDate desc
		""")
	Page<Comment> findActiveByRoomIdWithAuthor(@Param("roomId") Long roomId, Pageable pageable);

	Optional<Comment> findByIdAndChallengeRoom_Id(Long commentId, Long roomId);

	@EntityGraph(attributePaths = "user")
	Optional<Comment> findWithUserById(Long id);

	Optional<Comment> findByIdAndChallengeRoom_IdAndDeletedAtIsNull(Long commentId, Long roomId);
}
