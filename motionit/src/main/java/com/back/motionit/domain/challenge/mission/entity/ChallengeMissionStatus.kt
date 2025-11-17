package com.back.motionit.domain.challenge.mission.entity

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "challenge_mission_status",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["participant_id", "mission_date"])
    ]
)
class ChallengeMissionStatus(// 어떤 참가자의 기록인지
    @JoinColumn(name = "participant_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    var participant: ChallengeParticipant,

    // 해당 미션 날짜
    @Column(name = "mission_date", nullable = false)
    var missionDate: LocalDate,

    // 완료 여부
    @Column(nullable = false)
    var completed: Boolean = false,

    @Column(name = "ai_message", columnDefinition = "TEXT")
    var aiMessage: String? = null,
) : BaseEntity() {
    companion object {
        @JvmStatic
        fun create(participant: ChallengeParticipant, date: LocalDate): ChallengeMissionStatus {
            return ChallengeMissionStatus(
                participant = participant,
                missionDate = date,
                completed = false,
                aiMessage = null
            )
        }

        @JvmStatic
        fun create(participant: ChallengeParticipant, date: LocalDate, completed: Boolean): ChallengeMissionStatus {
            return ChallengeMissionStatus(
                participant = participant,
                missionDate = date,
                completed = completed,
                aiMessage = null
            )
        }
    }

    // 미션 완료 처리 메서드
    fun completeMission() {
        this.completed = true
    }

    fun updateAiMessage(message: String?) {
        this.aiMessage = message
    }
}
