package com.back.motionit.domain.challenge.mission.dto

import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole
import java.time.LocalDate

data class ChallengeMissionStatusResponse(
    val participantId: Long?,   // TODO: 현재 baseEntity가 호환성문제로 Long? 타입임, 추후 Long으로 변경예정
    val nickname: String,
    val userProfile: String?,
    val missionDate: LocalDate,
    val completed: Boolean,
    val role: ChallengeParticipantRole,
    val aiSummary: String?
) {
    companion object {
        @JvmStatic
        fun from(status: ChallengeMissionStatus): ChallengeMissionStatusResponse {
            val participant = status.participant
            val user = participant.user // TODO: 지연로딩으로 N+1 발생가능지점, 쿼리개선 필요

            return ChallengeMissionStatusResponse(
                participantId = participant.id,
                nickname = user.nickname,
                userProfile = user.userProfile,
                missionDate = status.missionDate,
                completed = status.completed,
                role = participant.role,
                aiSummary = status.aiMessage
            )
        }
    }
}
