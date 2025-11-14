package com.back.motionit.domain.challenge.participant.controller

import com.back.motionit.domain.challenge.participant.api.response.ChallengeParticipantHttp
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.user.entity.User
import com.back.motionit.factory.ChallengeRoomFactory.fakeChallengeRoom
import com.back.motionit.global.error.code.ChallengeParticipantErrorCode
import com.back.motionit.helper.UserHelper
import com.back.motionit.security.SecurityUser
import com.back.motionit.support.BaseIntegrationTest
import com.back.motionit.support.SecuredIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SecuredIntegrationTest
class ChallengeParticipantControllerTest : BaseIntegrationTest() {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var challengeParticipantRepository: ChallengeParticipantRepository

    @Autowired
    lateinit var challengeRoomRepository: ChallengeRoomRepository

    @Autowired
    lateinit var userHelper: UserHelper

    private lateinit var user: User
    private lateinit var room: ChallengeRoom

    lateinit var securityUser: SecurityUser
    lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setUp() {
        user = userHelper.createUser()
        room = createTestRoom(user)
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

    private fun createTestRoom(owner: User): ChallengeRoom {
        val room = fakeChallengeRoom(owner)
        return challengeRoomRepository.save(room)
    }

    @Test
    @DisplayName("POST `/api/v1/challenge/participants/{roomId}/join` - 성공적으로 방 참가")
    fun successJoinChallengeRoom() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/challenge/participants/{roomId}/join", room.id)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeParticipantController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("joinChallengeRoom"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(ChallengeParticipantHttp.JOIN_SUCCESS_MESSAGE))

        val participant =
            challengeParticipantRepository.findByUserAndChallengeRoom(user, room)

        Assertions.assertThat(participant).isNotNull()
        Assertions.assertThat(participant!!.quited).isFalse()
    }

    @Test
    @DisplayName("POST `/api/v1/challenge/participants/{roomId}/join` - 이미 참가중일 경우 실패")
    fun failWhenAlreadyJoined() {
        // given - 1회 참가
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/challenge/participants/{roomId}/join", room.id)
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(MockMvcResultHandlers.print())

        // when - 중복 참가
        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/challenge/participants/{roomId}/join", room.id)
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(MockMvcResultHandlers.print())

        val error = ChallengeParticipantErrorCode.ALREADY_JOINED

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeParticipantController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("joinChallengeRoom"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value(error.code))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(error.message))
    }

    @Test
    @DisplayName("POST `/api/v1/challenge/participants/{roomId}/join` - 존재하지 않는 방일 경우 실패")
    fun failWhenRoomNotFound() {
        val wrongRoomId = room.id!! + 1L

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/challenge/participants/{roomId}/join", wrongRoomId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(MockMvcResultHandlers.print())

        val error = ChallengeParticipantErrorCode.CANNOT_FIND_CHALLENGE_ROOM

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeParticipantController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("joinChallengeRoom"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value(error.code))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(error.message))
    }

    @Test
    @DisplayName("POST `/api/v1/challenge/participants/{roomId}/join` - 정원 초과 시 실패")
    fun failWhenRoomIsFull() {
        // given - 정원 2인 방
        val smallRoom = challengeRoomRepository.save(
            fakeChallengeRoom(user, 2)
        )

        val userA = userHelper.createUser()
        val userB = userHelper.createUser()
        challengeParticipantRepository.save(
            ChallengeParticipant(userA, smallRoom, ChallengeParticipantRole.NORMAL)
        )
        challengeParticipantRepository.save(
            ChallengeParticipant(userB, smallRoom, ChallengeParticipantRole.NORMAL)
        )

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/challenge/participants/{roomId}/join", smallRoom.id)
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(MockMvcResultHandlers.print())

        val error = ChallengeParticipantErrorCode.FULL_JOINED_ROOM

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeParticipantController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("joinChallengeRoom"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value(error.code))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(error.message))
    }

    @Test
    @DisplayName("POST `/api/v1/challenge/participants/{roomId}/leave` - 성공적으로 방 탈퇴")
    fun successLeaveChallengeRoom() {
        // given - 참가
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/challenge/participants/{roomId}/join", room.id)
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(MockMvcResultHandlers.print())

        // when - 탈퇴
        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/challenge/participants/{roomId}/leave", room.id)
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeParticipantController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("leaveChallengeRoom"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.resultCode").value(ChallengeParticipantHttp.LEAVE_SUCCESS_CODE)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(ChallengeParticipantHttp.LEAVE_SUCCESS_MESSAGE))

        val updated = challengeParticipantRepository
            .findByUserAndChallengeRoom(user, room)

        org.junit.jupiter.api.Assertions.assertNotNull(updated, "참가자가 조회되어야 합니다.")
        Assertions.assertThat(updated!!.quited).isTrue()
        Assertions.assertThat(updated.quitDate).isNotNull()
    }

    @Test
    @DisplayName("POST `/api/v1/challenge/participants/{roomId}/leave` - 방에 참가중이지 않으면 실패")
    fun failWhenNotParticipant() {
        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/challenge/participants/{roomId}/leave", room.id)
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(MockMvcResultHandlers.print())

        val error = ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeParticipantController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("leaveChallengeRoom"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value(error.code))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(error.message))
    }

    @DisplayName("GET `/api/v1/challenge/participants/{roomId}/status` - 현재 방 참가 상태 조회 성공")
    @Test
    fun participationStatus_success() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/challenge/participants/{roomId}/join", room.id)
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(MockMvcResultHandlers.print())

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/challenge/participants/{roomId}/status", room.id)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg")
                    .value(ChallengeParticipantHttp.GET_PARTICIPANT_STATUS_SUCCESS_MESSAGE)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.joined").value(true))
    }
}
