package com.back.motionit.domain.challenge.comment.controller

import com.back.motionit.domain.challenge.comment.dto.CommentCreateReq
import com.back.motionit.domain.challenge.comment.dto.CommentEditReq
import com.back.motionit.domain.challenge.comment.entity.Comment
import com.back.motionit.domain.challenge.comment.repository.CommentRepository
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import com.back.motionit.domain.challenge.video.entity.OpenStatus
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.helper.ChallengeParticipantHelper
import com.back.motionit.security.SecurityUser
import com.back.motionit.support.BaseIntegrationTest
import com.back.motionit.support.SecuredIntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@SecuredIntegrationTest
class CommentControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var om: ObjectMapper

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var roomRepository: ChallengeRoomRepository

    @Autowired
    lateinit var commentRepository: CommentRepository

    @Autowired
    lateinit var participantHelper: ChallengeParticipantHelper

    private lateinit var securityUser: SecurityUser
    private lateinit var authentication: UsernamePasswordAuthenticationToken
    private lateinit var user: User
    private lateinit var room: ChallengeRoom
    private var roomId: Long = 0L

    @BeforeEach
    fun setUp() {
        // 1) FK 끔
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE")

        // 2) 테이블 비우기 + 아이덴티티 리셋
        try {
            jdbc.execute("TRUNCATE TABLE room_comments RESTART IDENTITY")
        } catch (_: Exception) {
        }
        try {
            jdbc.execute("TRUNCATE TABLE challenge_rooms RESTART IDENTITY")
        } catch (_: Exception) {
        }
        try {
            jdbc.execute("TRUNCATE TABLE users RESTART IDENTITY")
        } catch (_: Exception) {
        }

        // 3) FK 켬
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE")

        // 4) 시드 (user #1)
        val u1 = User.builder()
            .email("u1@test.com")
            .nickname("u1")
            .password("pw")
            .loginType(LoginType.LOCAL)
            .userProfile(null)
            .build()
        user = userRepository.save(u1)

        // 5) 시드 (room #1)
        val r1 = ChallengeRoom(
            user,
            "Room-1",
            "Desc",
            100,
            OpenStatus.OPEN,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(30),
            "/img.png",
            null,
            ArrayList<ChallengeVideo>(),
            ArrayList<ChallengeParticipant>()
        )
        room = roomRepository.save(r1)
        participantHelper.createHostParticipant(user, room)
        roomId = requireNotNull(room.id)
    }

    private fun authenticateAs(user: User) {
        val authorities = AuthorityUtils.createAuthorityList("ROLE")
        securityUser = SecurityUser(user.id, user.password, user.nickname, authorities)
        authentication = UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Throws(Exception::class)
    private fun createComment(roomId: Long, content: String): Long {
        authenticateAs(user)

        val req = CommentCreateReq(content)
        val result = mockMvc.perform(
            post(BASE, roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req))
        )
            // .andExpect(status().isCreated())  // 원래 주석 처리되어 있던 부분
            .andExpect(jsonPath("$.resultCode").value("M-201"))
            .andExpect(jsonPath("$.data.id").exists())
            .andReturn()

        val body = result.response.contentAsString
        val node = om.readTree(body)["data"]
        return node["id"].asLong()
    }

    @Test
    @DisplayName("POST create -> HTTP200, resultCode=201-0, returns body")
    @Throws(Exception::class)
    fun create_comment() {
        authenticateAs(user)

        mockMvc.perform(
            post(BASE, roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(CommentCreateReq("첫 댓글")))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.resultCode").value("M-201"))
            .andExpect(jsonPath("$.data.roomId").value(roomId))
            .andExpect(jsonPath("$.data.authorId").value(user.id))
            .andExpect(jsonPath("$.data.authorNickname").value(user.nickname))
            .andExpect(jsonPath("$.data.content").value("첫 댓글"))
            .andExpect(jsonPath("$.data.deleted").value(false))
            .andExpect(jsonPath("$.data.likeCount").value(0))
    }

    @Test
    @DisplayName("GET list paged -> HTTP200, ResponseData<Page>")
    @Throws(Exception::class)
    fun list_paged() {
        for (i in 0 until 7) {
            createComment(roomId, "c$i")
        }

        mockMvc.perform(
            get(BASE, roomId)
                .param("page", "0")
                .param("size", "5")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("M-200"))
            .andExpect(jsonPath("$.data.content.length()").value(5))
            .andExpect(jsonPath("$.data.totalElements").value(7))
            .andExpect(jsonPath("$.data.totalPages").value(2))
            .andExpect(jsonPath("$.data.number").value(0))
    }

    @Test
    @DisplayName("PATCH edit -> HTTP200, content updated")
    @Throws(Exception::class)
    fun edit_comment() {
        val id = createComment(roomId, "old")

        mockMvc.perform(
            patch(ONE, roomId, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(CommentEditReq("new")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("M-200"))
            .andExpect(jsonPath("$.data.id").value(id))
            .andExpect(jsonPath("$.data.content").value("new"))
    }

    @Test
    @DisplayName("DELETE soft -> HTTP200(resultCode=204-0), list excludes deleted")
    @Throws(Exception::class)
    fun delete_comment_soft_excluded_from_list() {
        val id1 = createComment(roomId, "to-del-1")
        val id2 = createComment(roomId, "to-stay-2")

        mockMvc.perform(delete(ONE, roomId, id1))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("M-200"))

        mockMvc.perform(
            get(BASE, roomId)
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("M-200"))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].id").value(id2))
            .andExpect(jsonPath("$.data.content[0].content").value("to-stay-2"))
    }

    @Test
    @Throws(Exception::class)
    fun wrong_access_on_edit() {
        // user #2 생성
        val u2 = User.builder()
            .email("u2@test.com")
            .nickname("u2")
            .password("pw")
            .loginType(LoginType.LOCAL)
            .userProfile(null)
            .build()
        userRepository.save(u2)

        // room #1 참조
        val roomRef = roomRepository.getReferenceById(roomId)

        // author=2인 댓글 직접 저장 (Kotlin Comment 엔티티 사용)
        val comment = Comment(
            deletedAt = null,
            challengeRoom = roomRef,
            user = u2,
            content = "not-mine",
            likeCount = 0,
            version = null,
        )
        commentRepository.save(comment)
        val id = requireNotNull(comment.id)

        // 현재 컨트롤러는 로그인 유저(id=1) 기준 → WRONG_ACCESS 기대
        authenticateAs(user)

        mockMvc.perform(
            patch(ONE, roomId, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(CommentEditReq("should-fail")))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("M-101"))
    }

    @Test
    @DisplayName("검증에러 400 + NOT_FOUND 404 매핑 확인")
    @Throws(Exception::class)
    fun validation_and_notfound_cases() {
        // 400: POST content=""
        mockMvc.perform(
            post(BASE, roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(CommentCreateReq("")))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("C-002"))

        // 시드 1개
        val id = createComment(roomId, "ok")

        // 400: PATCH content=""
        mockMvc.perform(
            patch(ONE, roomId, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(CommentEditReq("")))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("C-002"))

        // 404: PATCH 잘못된 commentId
        mockMvc.perform(
            patch(ONE, roomId, 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(CommentEditReq("nope")))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("M-102"))

        // 404: DELETE 잘못된 commentId
        mockMvc.perform(delete(ONE, roomId, 999L))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("M-102"))
    }

    @Test
    @DisplayName("페이지네이션 정렬(내림차순) + 마지막/빈 페이지 + soft-delete 제외")
    @Throws(Exception::class)
    fun pagination_and_softdelete_behaviour() {
        // 7개 시드 (id 1..7)
        val ids = mutableListOf<Long>()
        for (i in 0 until 7) {
            ids.add(createComment(roomId, "c$i"))
        }

        // page=0,size=5 확인 (정렬: 최신 먼저)
        mockMvc.perform(
            get(BASE, roomId)
                .param("page", "0")
                .param("size", "5")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("M-200"))
            .andExpect(jsonPath("$.data.totalElements").value(7))
            .andExpect(jsonPath("$.data.totalPages").value(2))
            .andExpect(jsonPath("$.data.number").value(0))
            .andExpect(jsonPath("$.data.content.length()").value(5))
            // 내림차순: 첫 요소 id가 마지막 요소 id보다 커야 함 (단순 존재만 체크)
            .andExpect(jsonPath("$.data.content[0].id").exists())
            .andExpect(jsonPath("$.data.content[4].id").exists())

        // page=1,size=5 (마지막 페이지: 2개)
        mockMvc.perform(
            get(BASE, roomId)
                .param("page", "1")
                .param("size", "5")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("M-200"))
            .andExpect(jsonPath("$.data.number").value(1))
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.last").value(true))

        // page=2,size=5 (빈 페이지)
        mockMvc.perform(
            get(BASE, roomId)
                .param("page", "2")
                .param("size", "5")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("M-200"))
            .andExpect(jsonPath("$.data.content.length()").value(0))
            .andExpect(jsonPath("$.data.empty").value(true))

        // soft-delete: 하나 삭제 → 목록에서 제외됨 + totalElements 감소
        val toDelete = ids[0]
        mockMvc.perform(delete(ONE, roomId, toDelete))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("M-200"))

        mockMvc.perform(
            get(BASE, roomId)
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("M-200"))
            .andExpect(jsonPath("$.data.totalElements").value(6))
            // 삭제된 id가 목록에 없는지 확인
            .andExpect(
                jsonPath(
                    "\$.data.content[*].id",
                    not(hasItem(toDelete.toInt()))
                )
            )
    }

    companion object {
        private const val BASE = "/api/v1/rooms/{roomId}/comments"
        private const val ONE = "/api/v1/rooms/{roomId}/comments/{commentId}"
    }
}
