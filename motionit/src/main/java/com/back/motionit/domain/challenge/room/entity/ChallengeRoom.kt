package com.back.motionit.domain.challenge.room.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo;
import com.back.motionit.domain.challenge.video.entity.OpenStatus;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.global.jpa.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "challenge_rooms")
@SQLRestriction("deleted_at IS NULL")
public class ChallengeRoom extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	private String title;

	@Column(name = "description", length = 2000)
	private String description;
	private Integer capacity;

	@Enumerated(EnumType.STRING)
	private OpenStatus openStatus;

	@Column(name = "challenge_start_date")
	private LocalDateTime challengeStartDate;

	@Column(name = "challenge_end_date")
	private LocalDateTime challengeEndDate;

	@Column(name = "roome_image")
	private String roomImage; // 챌린지룸 이미지 URL

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@OneToMany(
		mappedBy = "challengeRoom",
		cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
		orphanRemoval = true,
		fetch = FetchType.LAZY
	)
	private List<ChallengeVideo> challengeVideoList = new ArrayList<>();

	@OneToMany(
		mappedBy = "challengeRoom",
		cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
		orphanRemoval = true,
		fetch = FetchType.LAZY
	)
	private List<ChallengeParticipant> participants = new ArrayList<>();

	@Transient
	public long getDDay() {
		if (challengeEndDate == null) {
			return 0;
		}

		LocalDate today = LocalDate.now();
		LocalDate end = this.challengeEndDate.toLocalDate();
		return ChronoUnit.DAYS.between(today, end);
	}
}
