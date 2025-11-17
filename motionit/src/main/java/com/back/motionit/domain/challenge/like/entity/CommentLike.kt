package com.back.motionit.domain.challenge.like.entity;

import com.back.motionit.domain.challenge.comment.entity.Comment;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.global.jpa.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "comment_likes",
	uniqueConstraints = {
		@UniqueConstraint(name = "unique_like", columnNames = {"comment_id", "user_id"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "comment_id", nullable = false)
	private Comment comment;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Builder
	public CommentLike(Comment comment, User user) {
		this.comment = comment;
		this.user = user;
	}
}
