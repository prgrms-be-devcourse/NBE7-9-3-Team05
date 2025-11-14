package com.back.motionit.domain.challenge.missionstatus.controller

import com.back.motionit.domain.challenge.mission.api.response.ChallengeMissionStatusHttp
import com.back.motionit.domain.challenge.mission.dto.ChallengeMissionCompleteRequest
import com.back.motionit.domain.challenge.mission.repository.ChallengeMissionStatusRepository
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository
import com.back.motionit.domain.user.entity.User
import com.back.motionit.factory.ChallengeMissionStatusFactory.fakeMission
import com.back.motionit.factory.ChallengeParticipantFactory.fakeParticipant
import com.back.motionit.factory.ChallengeRoomFactory.fakeChallengeRoom
import com.back.motionit.factory.ChallengeVideoFactory.fakeChallengeVideo
import com.back.motionit.global.error.code.ChallengeMissionErrorCode
import com.back.motionit.helper.UserHelper
import com.back.motionit.security.SecurityUser
import com.back.motionit.support.BaseIntegrationTest
import com.back.motionit.support.SecuredIntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate

@SecuredIntegrationTest
class ChallengeMissionsStatusControllerTest : BaseIntegrationTest() {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var challengeMissionStatusRepository: ChallengeMissionStatusRepository

    @Autowired
    lateinit var challengeParticipantRepository: ChallengeParticipantRepository

    @Autowired
    lateinit var challengeRoomRepository: ChallengeRoomRepository

    @Autowired
    lateinit var challengeVideoRepository: ChallengeVideoRepository

    @Autowired
    lateinit var userHelper: UserHelper

    private lateinit var room: ChallengeRoom
    private lateinit var participant: ChallengeParticipant
    private lateinit var video: ChallengeVideo
    private lateinit var today: LocalDate
    private lateinit var user: User

    private lateinit var securityUser: SecurityUser
    private lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setUp() {
        // 기본 사용자 및 데이터 구성
        user = userHelper.createUser()
        room = challengeRoomRepository.save(fakeChallengeRoom(user, 5))
        participant = challengeParticipantRepository.save(
            fakeParticipant(user, room)
        )
        video = challengeVideoRepository.save(fakeChallengeVideo(user, room))
        today = LocalDate.now()

        challengeMissionStatusRepository.save(
            fakeMission(
                participant
            )
        )

        // ChallengeRoomControllerTest와 동일한 인증 세팅
        val authorities = AuthorityUtils.createAuthorityList("ROLE_USER")
        securityUser = SecurityUser(user.id, user.password, user.nickname, authorities)
        authentication =
            UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("POST /rooms/{roomId}/missions/complete - 미션 완료 성공")
    fun completeMissionSuccess() {
        val request = ChallengeMissionCompleteRequest(video.id!!)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/challenge/rooms/{roomId}/missions/complete", room.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg")
                    .value(ChallengeMissionStatusHttp.MISSION_COMPLETE_SUCCESS_MESSAGE)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.completed").value(true))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("미션 완료 실패 - 이미 완료된 미션")
    fun completeMission_alreadyCompleted() {
        val mission = challengeMissionStatusRepository
            .findByParticipantIdAndMissionDate(participant.id!!, today)


        assertNotNull(mission, "테스트 미션 초기화 실패.")

        mission.completeMission()
        challengeMissionStatusRepository.save(mission)

        val request = ChallengeMissionCompleteRequest(video.id!!)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/challenge/rooms/{roomId}/missions/complete", room.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg").value(ChallengeMissionErrorCode.ALREADY_COMPLETED.message)
            )
            .andDo(MockMvcResultHandlers.print())
    }

    @DisplayName("GET /rooms/{roomId}/missions/personal/today - 개인 오늘 미션 조회 성공")
    @Test
    fun todayMissionStatus_success() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/challenge/rooms/{roomId}/missions/personal/today", room.id)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg")
                    .value(ChallengeMissionStatusHttp.GET_TODAY_PARTICIPANT_SUCCESS_MESSAGE)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.participantId").value(participant.id))
            .andDo(MockMvcResultHandlers.print())
    }

    @DisplayName("GET /rooms/{roomId}/missions/today - 방 전체 오늘의 미션 조회 성공")
    @Test
    fun todayMissionByRoom_success() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/challenge/rooms/{roomId}/missions/today", room.id)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg").value(ChallengeMissionStatusHttp.GET_TODAY_SUCCESS_MESSAGE)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
            .andDo(MockMvcResultHandlers.print())
    }

    @DisplayName("GET /rooms/{roomId}/missions/personal/history - 개인 미션 히스토리 조회 성공")
    @Test
    fun missionHistory_success() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/challenge/rooms/{roomId}/missions/personal/history", room.id)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg")
                    .value(ChallengeMissionStatusHttp.GET_MISSION_HISTORY_SUCCESS_MESSAGE)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
            .andDo(MockMvcResultHandlers.print())
    }
}
