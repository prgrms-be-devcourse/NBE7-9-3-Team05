package com.back.motionit.domain.challenge.missionstatus.controller

import com.back.motionit.domain.challenge.mission.api.response.ChallengeMissionStatusHttp
import com.back.motionit.domain.challenge.mission.dto.ChallengeMissionCompleteRequest
import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus
import com.back.motionit.domain.challenge.mission.repository.ChallengeMissionStatusRepository
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository
import com.back.motionit.domain.user.entity.User
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
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
    private lateinit var user: User
    private val today: LocalDate = LocalDate.now()

    private lateinit var securityUser: SecurityUser
    private lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setUp() {
        // 사용자 생성
        user = userHelper.createUser()

        // 방 & 참가자 & 영상 생성
        room = challengeRoomRepository.save(fakeChallengeRoom(user, 5))
        participant = challengeParticipantRepository.save(fakeParticipant(user, room))
        video = challengeVideoRepository.save(fakeChallengeVideo(user, room))

        // 오늘자 미션 1개 생성
        challengeMissionStatusRepository.save(ChallengeMissionStatus.create(participant, today))

        // SecurityContext 인증 설정
        val authorities = AuthorityUtils.createAuthorityList("ROLE_USER")
        securityUser = SecurityUser(user.id!!, user.password!!, user.nickname, authorities)
        authentication = UsernamePasswordAuthenticationToken(securityUser, null, authorities)

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
            post("/api/v1/challenge/rooms/{roomId}/missions/complete", room.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.msg")
                    .value(ChallengeMissionStatusHttp.MISSION_COMPLETE_SUCCESS_MESSAGE)
            )
            .andExpect(jsonPath("$.data.completed").value(true))
            .andDo(print())
    }

    @Test
    @DisplayName("미션 완료 실패 - 이미 완료된 미션")
    fun completeMission_alreadyCompleted() {
        val mission = challengeMissionStatusRepository
            .findByParticipantIdAndMissionDate(participant.id!!, today)
        requireNotNull(mission) { "TEST SETUP FAILED: mission must not be null" }

        assertNotNull(mission, "테스트 미션 초기화 실패.")

        mission.completeMission()
        challengeMissionStatusRepository.save(mission)

        val request = ChallengeMissionCompleteRequest(video.id!!)

        mvc.perform(
            post("/api/v1/challenge/rooms/{roomId}/missions/complete", room.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().is4xxClientError())
            .andExpect(
                jsonPath("$.msg").value(ChallengeMissionErrorCode.ALREADY_COMPLETED.message)
            )
            .andDo(print())
    }

    @DisplayName("GET /rooms/{roomId}/missions/personal/today - 개인 오늘 미션 조회 성공")
    @Test
    fun todayMissionStatus_success() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/challenge/rooms/{roomId}/missions/personal/today", room.id)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.msg")
                    .value(ChallengeMissionStatusHttp.GET_TODAY_PARTICIPANT_SUCCESS_MESSAGE)
            )
            .andExpect(jsonPath("$.data.participantId").value(participant.id))
            .andDo(print())
    }

    @DisplayName("GET /rooms/{roomId}/missions/today - 방 전체 오늘의 미션 조회 성공")
    @Test
    fun todayMissionByRoom_success() {
        mvc.perform(
            get("/api/v1/challenge/rooms/{roomId}/missions/today", room.id)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.msg").value(ChallengeMissionStatusHttp.GET_TODAY_SUCCESS_MESSAGE)
            )
            .andExpect(jsonPath("$.data").isArray())
            .andDo(print())
    }

    @DisplayName("GET /rooms/{roomId}/missions/personal/history - 개인 미션 히스토리 조회 성공")
    @Test
    fun missionHistory_success() {
        mvc.perform(
            get("/api/v1/challenge/rooms/{roomId}/missions/personal/history", room.id)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.msg")
                    .value(ChallengeMissionStatusHttp.GET_MISSION_HISTORY_SUCCESS_MESSAGE)
            )
            .andExpect(jsonPath("$.data").isArray())
            .andDo(print())
    }
}
