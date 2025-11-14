package com.back.motionit.domain.challenge.video.controller

import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.challenge.video.api.response.ChallengeVideoHttp
import com.back.motionit.domain.challenge.video.dto.ChallengeVideoUploadRequest
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository
import com.back.motionit.domain.user.entity.User
import com.back.motionit.factory.ChallengeParticipantFactory.fakeParticipant
import com.back.motionit.factory.ChallengeRoomFactory.fakeChallengeRoom
import com.back.motionit.factory.ChallengeVideoFactory.fakeChallengeVideo
import com.back.motionit.global.error.code.ChallengeParticipantErrorCode
import com.back.motionit.helper.UserHelper
import com.back.motionit.security.SecurityUser
import com.back.motionit.support.BaseIntegrationTest
import com.back.motionit.support.SecuredIntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import java.util.function.Consumer

@SecuredIntegrationTest
internal class ChallengeVideoControllerTest: BaseIntegrationTest() {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var challengeVideoRepository: ChallengeVideoRepository

    @Autowired
    lateinit var challengeRoomRepository: ChallengeRoomRepository

    @Autowired
    lateinit var challengeParticipantRepository: ChallengeParticipantRepository

    @Autowired
    lateinit var userHelper: UserHelper

    @Autowired
    lateinit var adapter: RequestMappingHandlerAdapter

    private lateinit var user: User
    private lateinit var room: ChallengeRoom

    private lateinit var securityUser: SecurityUser
    private lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setUp() {
        challengeVideoRepository.deleteAll()

        // 기본 사용자 및 방, 참가자 세팅
        user = userHelper.createUser()
        room = challengeRoomRepository.save(fakeChallengeRoom(user, 5))
        challengeParticipantRepository.save(
            fakeParticipant(user, room)
        )

        // 인증 세팅 (ChallengeRoomControllerTest와 동일)
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
    fun printConverters() {
        adapter.messageConverters.forEach(
            Consumer { c: HttpMessageConverter<*> -> println("✅ " + c.javaClass.name) }
        )
    }

    @Test
    @DisplayName("POST `/api/v1/challenge/rooms/{roomId}/videos` - 영상 업로드 성공")
    fun successUploadVideo() {
        // given
        val request = ChallengeVideoUploadRequest(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        )
        val requestJson = objectMapper.writeValueAsString(request)

        // when & then
        mvc.perform(
            post("/api/v1/challenge/rooms/{roomId}/videos", room.id)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeVideoController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("uploadVideo"))
            .andExpect(jsonPath("$.msg").value(ChallengeVideoHttp.UPLOAD_SUCCESS_MESSAGE))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("POST `/api/v1/challenge/rooms/{roomId}/videos` - 참가자가 아닌 경우 업로드 실패")
    fun failUploadVideo_NotParticipant() {
        // given - 다른 방 생성, 인증유저는 참여 안함
        val otherRoom = challengeRoomRepository.save(
            fakeChallengeRoom(userHelper.createUser(), 5)
        )

        val request = ChallengeVideoUploadRequest(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        )
        val requestJson = objectMapper.writeValueAsString(request)

        // when & then
        mvc.perform(
            post("/api/v1/challenge/rooms/{roomId}/videos", otherRoom.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
            .andExpect(
                jsonPath("$.msg")
                    .value(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM.message)
            )
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("GET `/api/v1/challenge/rooms/{roomId}/videos/today` - 오늘의 미션 영상 조회 성공")
    fun successGetTodayMissionVideos() {
        // given
        challengeVideoRepository.save(fakeChallengeVideo(user, room))
        challengeVideoRepository.save(fakeChallengeVideo(user, room))

        // when & then
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/challenge/rooms/{roomId}/videos/today", room.id)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeVideoController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getTodayMissionVideos"))
            .andExpect(
                jsonPath("$.msg").value(ChallengeVideoHttp.GET_TODAY_MISSION_SUCCESS_MESSAGE)
            )
            .andExpect(jsonPath("$.data").isArray())
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("GET `/api/v1/challenge/rooms/{roomId}/videos/today` - 참가자가 아닌 경우 실패")
    fun failGetTodayMissionVideos_NotParticipant() {
        val otherRoom = challengeRoomRepository.save(
            fakeChallengeRoom(userHelper.createUser(), 5)
        )

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/challenge/rooms/{roomId}/videos/today", otherRoom.id)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
            .andExpect(
                jsonPath("$.msg")
                    .value(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM.message)
            )
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("DELETE `/api/v1/challenge/rooms/{roomId}/videos/{videoId}` - 영상 삭제 성공")
    fun successDeleteVideo() {
        // given
        val video: ChallengeVideo = challengeVideoRepository.save(
            fakeChallengeVideo(user, room)
        )

        // when & then
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/challenge/rooms/{roomId}/videos/{videoId}", room.id, video.id)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeVideoController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("deleteVideoByUser"))
            .andExpect(jsonPath("$.msg").value(ChallengeVideoHttp.DELETE_SUCCESS_MESSAGE))
            .andDo(MockMvcResultHandlers.print())

        Assertions.assertThat(challengeVideoRepository.findById(video.id!!)).isEmpty()
    }

    @Test
    @DisplayName("DELETE `/api/v1/challenge/rooms/{roomId}/videos/{videoId}` - 참가자가 아닐 경우 삭제 실패")
    fun failDeleteVideo_NotParticipant() {
        // given
        val otherUser = userHelper.createUser()
        val otherRoom = challengeRoomRepository.save(fakeChallengeRoom(otherUser, 5))
        val video = challengeVideoRepository.save(
            fakeChallengeVideo(otherUser, otherRoom)
        )

        // when & then
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/challenge/rooms/{roomId}/videos/{videoId}", otherRoom.id, video.id)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
            .andExpect(
                jsonPath("$.msg")
                    .value(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM.message)
            )
            .andDo(MockMvcResultHandlers.print())
    }
}
