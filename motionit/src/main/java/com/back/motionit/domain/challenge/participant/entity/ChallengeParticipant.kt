package com.back.motionit.domain.challenge.participant.entity

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.user.entity.User
import com.back.motionit.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "challenge_participants",
    indexes = [
        Index(
            name = "idx_cp_user_room_quited",
            columnList = "user_id, challenge_room_id, quited"
        )
    ]
)
class ChallengeParticipant(// 챌린지 참가자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_room_id", nullable = false)
    var challengeRoom: ChallengeRoom,

    // 챌린지 참가자의 역할 (NORMAL, ADMIN)
    @Column(nullable = false)
    var role: ChallengeParticipantRole,

    @Column(nullable = false, name = "challenge_status")
    var challengeStatus: Boolean = false,

    @Column(name = "quit_date")
    var quitDate: LocalDateTime? = null,

    @Column(name = "quited", nullable = false)
    var quited: Boolean = false,

) : BaseEntity() {

    companion object {
        // Java의 builder() 대체하는 Factory
        @JvmStatic
        fun create(
            user: User,
            room: ChallengeRoom,
            role: ChallengeParticipantRole,
            quited: Boolean = false,
            challengeStatus: Boolean = false,
            quitDate: LocalDateTime? = null
        ): ChallengeParticipant {
            return ChallengeParticipant(
                user = user,
                challengeRoom = room,
                role = role,
                quited = quited,
                challengeStatus = challengeStatus,
                quitDate = quitDate
            )
        }
    }

    constructor(user: User, room: ChallengeRoom, role: ChallengeParticipantRole): this(
        user = user,
        challengeRoom = room,
        role = role,
        challengeStatus = false,
        quitDate = null,
        quited = false
    )

    fun quitChallenge() {
        this.quited = true
        this.quitDate = LocalDateTime.now()
    }
}
