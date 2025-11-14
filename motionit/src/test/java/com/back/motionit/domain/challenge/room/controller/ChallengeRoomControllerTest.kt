package com.back.motionit.domain.challenge.room.controller

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.room.api.response.ChallengeRoomHttp
import com.back.motionit.domain.challenge.room.builder.CreateRoomRequestBuilder
import com.back.motionit.domain.challenge.room.dto.GetRoomsResponse
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.user.entity.User
import com.back.motionit.global.constants.ChallengeRoomConstants
import com.back.motionit.global.error.code.ChallengeRoomErrorCode
import com.back.motionit.global.error.code.CommonErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.helper.ChallengeParticipantHelper
import com.back.motionit.helper.ChallengeRoomHelper
import com.back.motionit.helper.UserHelper
import com.back.motionit.security.SecurityUser
import com.back.motionit.support.BaseIntegrationTest
import com.back.motionit.support.SecuredIntegrationTest
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions
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

@SecuredIntegrationTest
class ChallengeRoomControllerTest : BaseIntegrationTest() {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var challengeRoomRepository: ChallengeRoomRepository

    @Autowired
    lateinit var challengeParticipantRepository: ChallengeParticipantRepository

    @Autowired
    lateinit var userHelper: UserHelper

    @Autowired
    lateinit var roomHelper: ChallengeRoomHelper

    @Autowired
    lateinit var participantHelper: ChallengeParticipantHelper

    @Autowired
    lateinit var mapper: ObjectMapper

    private lateinit var createRoomRequestBuilder: CreateRoomRequestBuilder
    private lateinit var user: User

    lateinit var securityUser: SecurityUser
    lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setUp() {
        createRoomRequestBuilder = CreateRoomRequestBuilder("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        user = userHelper.createUser()
    }

    @Nested
    @DisplayName("POST `/api/v1/challenge/rooms` - 운동방 생성")
    internal inner class CreateRoomTest {
        private val baseRoomApi = "/api/v1/challenge/rooms"

        @Test
        @DisplayName("Success Create Challenge Room")
        fun successCreateRoom() {
            val params = createRoomRequestBuilder.toParamMap()

            val authorities = AuthorityUtils.createAuthorityList("ROLE")
            securityUser = SecurityUser(user.id, user.password, user.nickname, authorities)
            authentication =
                UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
            SecurityContextHolder.getContext().authentication = authentication

            val requestJson = mapper.writeValueAsString(
                mapOf(
                    "title" to params["title"],
                    "description" to params["description"],
                    "capacity" to params["capacity"]!!.toInt(),
                    "duration" to params["duration"]!!.toInt(),
                    "videoUrl" to params["videoUrl"],
                    "imageFileName" to params["imageFileName"],
                    "contentType" to params["contentType"],
                )
            )

            val resultActions = mvc.perform(
                MockMvcRequestBuilders.post(baseRoomApi)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            ).andDo(MockMvcResultHandlers.print())

            resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeRoomController::class.java))
                .andExpect(MockMvcResultMatchers.handler().methodName("createRoom"))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.resultCode").value(ChallengeRoomHttp.CREATE_ROOM_SUCCESS_CODE)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(ChallengeRoomHttp.CREATE_ROOM_SUCCESS_MESSAGE))

            val mvcResult = resultActions.andReturn()

            val responseJson = mvcResult.response.contentAsString
            val title = JsonPath.read<String>(responseJson, "$.data.title")
            val image = JsonPath.read<String>(responseJson, "$.data.roomImage")
            val id = JsonPath.read<Number>(responseJson, "$.data.id").toLong()

            val createdRoom =
                challengeRoomRepository.findById(id).orElseThrow { BusinessException(CommonErrorCode.NOT_FOUND) }

            Assertions.assertThat(title).isEqualTo(params["title"])
            Assertions.assertThat(createdRoom.roomImage).isEqualTo(image)
        }

        @Test
        @DisplayName("Failed with NOT FOUND USER")
        fun notFoundUserId() {
            val params = createRoomRequestBuilder.toParamMap()

            // TODO: BaseEntity 리팩토링 완료 후 수정 필요
            val wrongUserId = user.id!! + 1L
            val authorities = AuthorityUtils.createAuthorityList("ROLE")
            securityUser = SecurityUser(wrongUserId, user.password, user.nickname, authorities)
            authentication =
                UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
            SecurityContextHolder.getContext().authentication = authentication

            val requestJson = mapper.writeValueAsString(
                mapOf(
                    "title" to params["title"],
                    "description" to params["description"],
                    "capacity" to params["capacity"]!!.toInt(),
                    "duration" to params["duration"]!!.toInt(),
                    "videoUrl" to params["videoUrl"],
                    "imageFileName" to params["imageFileName"],
                    "contentType" to params["contentType"],
                )
            )

            val resultActions = mvc.perform(
                MockMvcRequestBuilders.post(baseRoomApi)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            ).andDo(MockMvcResultHandlers.print())

            val error = ChallengeRoomErrorCode.NOT_FOUND_USER

            resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeRoomController::class.java))
                .andExpect(MockMvcResultMatchers.handler().methodName("createRoom"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value(error.code))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(error.message))
        }

        @Test
        @DisplayName("Auto Join as Host after Create Room")
        fun autoJoinAsHostAfterCreateRoom() {
            // given
            val owner = userHelper.createUser()
            val params = createRoomRequestBuilder.toParamMap()

            val authorities = AuthorityUtils.createAuthorityList("ROLE_USER")
            securityUser = SecurityUser(owner.id, owner.password, owner.nickname, authorities)
            authentication =
                UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
            SecurityContextHolder.getContext().authentication = authentication

            val requestJson = mapper.writeValueAsString(
                mapOf(
                    "title" to params["title"],
                    "description" to params["description"],
                    "capacity" to params["capacity"]!!.toInt(),
                    "duration" to params["duration"]!!.toInt(),
                    "videoUrl" to params["videoUrl"],
                    "imageFileName" to params["imageFileName"],
                    "contentType" to params["contentType"],
                )
            )

            // when
            val result = mvc.perform(
                MockMvcRequestBuilders.post(baseRoomApi)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.resultCode").value(ChallengeRoomHttp.CREATE_ROOM_SUCCESS_CODE)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(ChallengeRoomHttp.CREATE_ROOM_SUCCESS_MESSAGE))
                .andReturn()

            // then
            val responseJson = result.response.contentAsString
            val roomId = JsonPath.read<Number>(responseJson, "$.data.id").toLong()
            val createdRoom = challengeRoomRepository.findById(roomId)
                .orElseThrow()
            val participant = challengeParticipantRepository
                .findByUserAndChallengeRoom(owner, createdRoom)

            org.junit.jupiter.api.Assertions.assertNotNull(participant, "참가자가 조회되어야 합니다.")
            Assertions.assertThat(participant!!.role).isEqualTo(ChallengeParticipantRole.HOST)
        }
    }

    @Nested
    @DisplayName("GET `/api/v1/challenge/rooms` - 운동방 전체 목록 조회")
    internal inner class GetRoomsTest {
        private val baseRoomApi = "/api/v1/challenge/rooms"

        @Test
        @DisplayName("운동방 목록 조회 성공, page=0 & size=20")
        fun successGetRoomsWithParams() {
            val page = 0
            val size = 20

            for (i in 0 until size + 1) {
                roomHelper.createChallengeRoom(user)
            }

            val authorities = AuthorityUtils.createAuthorityList("ROLE")
            securityUser = SecurityUser(user.id, user.password, user.nickname, authorities)
            authentication =
                UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
            SecurityContextHolder.getContext().authentication = authentication

            val resultActions = mvc.perform(
                MockMvcRequestBuilders.get(baseRoomApi)
                    .param("page", page.toString())
                    .param("size", size.toString())
                    .contentType(MediaType.APPLICATION_JSON)
            ).andDo(MockMvcResultHandlers.print())

            resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeRoomController::class.java))
                .andExpect(MockMvcResultMatchers.handler().methodName("getRooms"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.resultCode").value(ChallengeRoomHttp.GET_ROOMS_SUCCESS_CODE)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(ChallengeRoomHttp.GET_ROOMS_SUCCESS_MESSAGE))

            val mvcResult = resultActions.andReturn()
            val responseJson = mvcResult.response.contentAsString
            val raw = JsonPath.read<Any>(responseJson, "$.data")
            val data = mapper.convertValue(raw,
                object : TypeReference<GetRoomsResponse>() {
                }
            )

            Assertions.assertThat(data.rooms).hasSizeLessThanOrEqualTo(size)
        }

        @Test
        @DisplayName("운동방 목록 조회 성공, default 쿼리 params")
        fun successGetRoomsWithDefault() {
            for (i in 0..19) {
                roomHelper.createChallengeRoom(user)
            }

            val authorities = AuthorityUtils.createAuthorityList("ROLE")
            securityUser = SecurityUser(user.id, user.password, user.nickname, authorities)
            authentication =
                UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
            SecurityContextHolder.getContext().authentication = authentication

            val resultActions = mvc.perform(
                MockMvcRequestBuilders.get(baseRoomApi)
                    .contentType(MediaType.APPLICATION_JSON)
            ).andDo(MockMvcResultHandlers.print())

            resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeRoomController::class.java))
                .andExpect(MockMvcResultMatchers.handler().methodName("getRooms"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.resultCode").value(ChallengeRoomHttp.GET_ROOMS_SUCCESS_CODE)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(ChallengeRoomHttp.GET_ROOMS_SUCCESS_MESSAGE))

            val mvcResult = resultActions.andReturn()
            val responseJson = mvcResult.response.contentAsString
            val raw = JsonPath.read<Any>(responseJson, "$.data")
            val data = mapper.convertValue(raw,
                object : TypeReference<GetRoomsResponse>() {
                }
            )

            Assertions.assertThat(data.rooms).hasSizeLessThanOrEqualTo(ChallengeRoomConstants.DEFAULT_SIZE.toInt())
        }
    }

    @Nested
    @DisplayName("GET `/api/v1/challenge/rooms/{roomId}` - 운동방 상세 조회")
    internal inner class GetRoomTest {
        private val getRoomApi = "/api/v1/challenge/rooms/{roomId}"

        @Test
        @DisplayName("운동방 조회 성공")
        fun successGetRoom() {
            val room = roomHelper.createChallengeRoom(user)

            val resultActions = mvc.perform(
                MockMvcRequestBuilders.get(getRoomApi, room.id)
                    .contentType(MediaType.APPLICATION_JSON)
            ).andDo(MockMvcResultHandlers.print())

            resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeRoomController::class.java))
                .andExpect(MockMvcResultMatchers.handler().methodName("getRoom"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.resultCode").value(ChallengeRoomHttp.GET_ROOM_SUCCESS_CODE)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(ChallengeRoomHttp.GET_ROOM_SUCCESS_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(room.id))
        }

        @Test
        @DisplayName("운동방 조회 실패 - 잘못된 roomId")
        fun failedGetRoomWithWrongId() {
            val room = roomHelper.createChallengeRoom(user)

            val resultActions = mvc.perform(
                MockMvcRequestBuilders.get(getRoomApi, room.id!! + 1)
                    .contentType(MediaType.APPLICATION_JSON)
            ).andDo(MockMvcResultHandlers.print())

            val error = ChallengeRoomErrorCode.NOT_FOUND_ROOM

            resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeRoomController::class.java))
                .andExpect(MockMvcResultMatchers.handler().methodName("getRoom"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value(error.code))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(error.message))
        }
    }

    @Nested
    @DisplayName("DELETE `/api/v1/challenge/rooms/{roomId}` - 운동방 삭제")
    internal inner class DeleteRoomTest {
        private val deleteRoomApi = "/api/v1/challenge/rooms/{roomId}"

        @Test
        @DisplayName("운동방 삭제 성공")
        fun successDeleteRoom() {
            val authorities = AuthorityUtils.createAuthorityList("ROLE")
            securityUser = SecurityUser(user.id, user.password, user.nickname, authorities)
            authentication =
                UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
            SecurityContextHolder.getContext().authentication = authentication

            val room = roomHelper.createChallengeRoom(user)
            participantHelper.createHostParticipant(user, room)

            val resultActions = mvc.perform(
                MockMvcRequestBuilders.delete(deleteRoomApi, room.id)
                    .contentType(MediaType.APPLICATION_JSON)
            ).andDo(MockMvcResultHandlers.print())

            resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeRoomController::class.java))
                .andExpect(MockMvcResultMatchers.handler().methodName("deleteRoom"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.resultCode").value(ChallengeRoomHttp.DELETE_ROOM_SUCCESS_CODE)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(ChallengeRoomHttp.DELETE_ROOM_SUCCESS_MESSAGE))

            Assertions.assertThat(challengeRoomRepository.findById(room.id!!)).isEmpty()

            val deletedAt = challengeRoomRepository.findDeletedAtRaw(room.id!!)
            Assertions.assertThat(deletedAt).isNotNull()
        }

        @Test
        @DisplayName("운동방 삭제 실패 - 일반 참여자 권한 거부")
        fun failedDeleteRoomWithRole() {
            val authorities = AuthorityUtils.createAuthorityList("ROLE")
            securityUser = SecurityUser(user.id, user.password, user.nickname, authorities)
            authentication =
                UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
            SecurityContextHolder.getContext().authentication = authentication

            val room = roomHelper.createChallengeRoom(user)
            participantHelper.createNormalParticipant(user, room)

            val resultActions = mvc.perform(
                MockMvcRequestBuilders.delete(deleteRoomApi, room.id)
                    .contentType(MediaType.APPLICATION_JSON)
            ).andDo(MockMvcResultHandlers.print())

            val error = ChallengeRoomErrorCode.INVALID_AUTH_USER

            resultActions
                .andExpect(MockMvcResultMatchers.handler().handlerType(ChallengeRoomController::class.java))
                .andExpect(MockMvcResultMatchers.handler().methodName("deleteRoom"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value(error.code))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value(error.message))
        }
    }

    @AfterEach
    fun clear() {
        SecurityContextHolder.clearContext()
    }
}
