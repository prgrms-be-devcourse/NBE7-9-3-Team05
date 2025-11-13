package com.back.motionit.domain.challenge.participant.entity;

import java.time.LocalDateTime;

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Table(name = "challenge_participants")
public class ChallengeParticipant extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;  // 챌린지 참가자

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "challenge_room_id", nullable = false)
	private ChallengeRoom challengeRoom;

	@Column(name = "quit_date")
	private LocalDateTime quitDate; // 참가자가 운동방을 탈퇴한 날짜
	@Column(name = "quited", nullable = false)
	private Boolean quited; // 참가자의 운동방 탈퇴여부

	@Column(nullable = false)
	private ChallengeParticipantRole role; // 챌린지 참가자의 역할 (예: NORMAL, ADMIN)

	@Column(nullable = false, name = "challenge_status")
	private Boolean challengeStatus = false; // 챌린지 참가자의 챌린지 상태 (예: 진행 중, 완료 등)

	// TODO: 불리안 타입의 challengeStatus은 오늘 완료와 내일 미완료 구분을 못함 추후 별도 엔티티로 관리 필요

	public ChallengeParticipant(User user, ChallengeRoom challengeRoom,
		ChallengeParticipantRole challengeParticipantRole) {
		this.user = user;
		this.challengeRoom = challengeRoom;
		this.quited = false;
		this.role = challengeParticipantRole;
	}

	public void quitChallenge() {
		this.quited = true;
		this.quitDate = LocalDateTime.now();
	}
}
