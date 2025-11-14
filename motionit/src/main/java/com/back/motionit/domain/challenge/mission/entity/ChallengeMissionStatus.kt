package com.back.motionit.domain.challenge.mission.entity;

import java.time.LocalDate;

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
	name = "challenge_mission_status",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"participant_id", "mission_date"})
	}
)
public class ChallengeMissionStatus extends BaseEntity {
	// 어떤 참가자의 기록인지
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "participant_id", nullable = false)
	private ChallengeParticipant participant;

	// 해당 미션 날짜
	@Column(name = "mission_date", nullable = false)
	private LocalDate missionDate;

	// 완료 여부
	@Column(nullable = false)
	private Boolean completed;

	@Column(name = "ai_message", columnDefinition = "TEXT")
	private String aiMessage;

	public ChallengeMissionStatus(ChallengeParticipant participant, LocalDate today) {
		this.participant = participant;
		this.missionDate = today;
		this.completed = false;
		this.aiMessage = null;
	}

	// 미션 완료 처리 메서드
	public void completeMission() {
		this.completed = true;
	}

	public void setAiMessage(String aiMessage) {
		this.aiMessage = aiMessage;
	}
}
