package com.back.motionit.domain.challenge.room.entity

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import com.back.motionit.domain.challenge.video.entity.OpenStatus
import com.back.motionit.domain.user.entity.User
import com.back.motionit.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

@Entity
@Table(name = "challenge_rooms")
@SQLRestriction("deleted_at IS NULL")
class ChallengeRoom(
    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "user_id", nullable = false)
    var user: User,

	var title: String,

    @field:Column(name = "description", length = 2000)
	var description: String,

    var capacity: Int,

	@field:Enumerated(EnumType.STRING)
	var openStatus: OpenStatus,

	@field:Column(name = "challenge_start_date")
	var challengeStartDate: LocalDateTime,

	@field:Column(name = "challenge_end_date")
	var challengeEndDate: LocalDateTime,

	@field:Column(name = "roome_image")
	var roomImage: String,

	@field:Column(name = "deleted_at", nullable = true)
	var deletedAt: LocalDateTime? = null
) : BaseEntity() {
    @OneToMany(
        mappedBy = "challengeRoom",
        cascade = [CascadeType.PERSIST, CascadeType.REMOVE],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private val challengeVideoList: MutableList<ChallengeVideo> = mutableListOf()

    @OneToMany(
        mappedBy = "challengeRoom",
        cascade = [CascadeType.PERSIST, CascadeType.REMOVE],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private val participants: MutableList<ChallengeParticipant> = mutableListOf()

    fun getChallengeVideoList(): List<ChallengeVideo> = challengeVideoList
    fun getParticipants(): List<ChallengeParticipant> = participants
}
