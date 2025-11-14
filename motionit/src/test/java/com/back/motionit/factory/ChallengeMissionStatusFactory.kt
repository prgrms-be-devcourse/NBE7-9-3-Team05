package com.back.motionit.factory

import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import java.time.LocalDate

object ChallengeMissionStatusFactory : BaseFactory() {
    /**
     * 커스텀 날짜 및 완료 여부 지정 가능
     */
    /**
     * 오늘 날짜 기준 미완료 미션 생성
     */
    fun fakeMission(
        participant: ChallengeParticipant,
        missionDate: LocalDate = LocalDate.now(),
        completed: Boolean = false
    ): ChallengeMissionStatus {
        return ChallengeMissionStatus.create(participant, missionDate, completed)
    }
}
