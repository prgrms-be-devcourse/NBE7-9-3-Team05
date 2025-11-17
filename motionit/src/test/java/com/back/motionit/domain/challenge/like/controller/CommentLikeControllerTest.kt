package com.back.motionit.domain.challenge.like.controller

import com.back.motionit.domain.challenge.comment.entity.Comment
import com.back.motionit.domain.challenge.comment.repository.CommentRepository
import com.back.motionit.domain.challenge.like.entity.CommentLike
import com.back.motionit.domain.challenge.like.repository.CommentLikeRepository
import com.back.motionit.domain.challenge.like.service.CommentLikeService
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.user.entity.User
import com.back.motionit.helper.ChallengeParticipantHelper
import com.back.motionit.helper.ChallengeRoomHelper
import com.back.motionit.helper.CommentHelper
import com.back.motionit.helper.UserHelper
import com.back.motionit.security.SecurityUser
import com.back.motionit.support.BaseIntegrationTest
import com.back.motionit.support.SecuredIntegrationTest
import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SecuredIntegrationTest
class CommentLikeControllerTest : BaseIntegrationTest() {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var commentRepository: CommentRepository
    @Autowired lateinit var commentLikeRepository: CommentLikeRepository
    @Autowired lateinit var userHelper: UserHelper
    @Autowired lateinit var roomHelper: ChallengeRoomHelper
    @Autowired lateinit var commentHelper: CommentHelper
    @Autowired lateinit var participantHelper: ChallengeParticipantHelper
    @Autowired lateinit var commentLikeService: CommentLikeService

    private lateinit var user: User
    private lateinit var room: ChallengeRoom
    private lateinit var comment: Comment
    private lateinit var securityUser: SecurityUser
    private lateinit var authentication: UsernamePasswordAuthenticationToken

    private val toggleLikeApi = "/api/v1/comments/{commentId}/likes"
    private val getCommentsApi = "/api/v1/rooms/{roomId}/comments"
    private val deleteCommentApi = "/api/v1/rooms/{roomId}/comments/{commentId}"

    @BeforeEach
    fun setUp() {
        user = userHelper.createUser()
        room = roomHelper.createChallengeRoom(user)
        comment = commentHelper.createComment(user, room, "테스트 댓글입니다.")

        val authorities = AuthorityUtils.createAuthorityList("ROLE_USER")
        securityUser = SecurityUser(user.id!!, user.password!!, user.nickname!!, authorities)
        authentication = UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Nested
    @DisplayName("POST `/api/v1/comments/{commentId}/likes` - 댓글 좋아요 토글")
    inner class ToggleCommentLikeTest {

        @Test
        @DisplayName("좋아요 생성 성공")
        fun successCreateLike() {
            mvc.perform(
                post(toggleLikeApi, comment.id)
                    .contentType(MediaType.APPLICATION_JSON)
            )

            val updatedComment = commentRepository.findById(comment.id!!).orElseThrow()
            assertThat(updatedComment.likeCount).isEqualTo(1)
            assertThat(commentLikeRepository.existsByCommentAndUser(updatedComment, user)).isTrue()
        }

        @Test
        @DisplayName("좋아요 취소 성공")
        fun successCancelLike() {
            val like = CommentLike.create(comment, user)
            commentLikeRepository.save(like)
            comment.incrementLikeCount()
            commentRepository.save(comment)

            mvc.perform(
                post(toggleLikeApi, comment.id)
                    .contentType(MediaType.APPLICATION_JSON)
            )

            val updatedComment = commentRepository.findById(comment.id!!).orElseThrow()
            assertThat(updatedComment.likeCount).isEqualTo(0)
            assertThat(commentLikeRepository.existsByCommentAndUser(updatedComment, user)).isFalse()
        }

        @Test
        @DisplayName("좋아요 토글 여러 번 수행")
        fun toggleLikeMultipleTimes() {
            mvc.perform(post(toggleLikeApi, comment.id).contentType(MediaType.APPLICATION_JSON))
            mvc.perform(post(toggleLikeApi, comment.id).contentType(MediaType.APPLICATION_JSON))
            mvc.perform(post(toggleLikeApi, comment.id).contentType(MediaType.APPLICATION_JSON))
            val finalComment = commentRepository.findById(comment.id!!).orElseThrow()
            assertThat(finalComment.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("존재하지 않는 댓글에 좋아요 시도 - 실패")
        fun failedLikeNonExistentComment() {
            val nonExistentCommentId = comment.id!! + 999L
            mvc.perform(
                post(toggleLikeApi, nonExistentCommentId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andDo(print())
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value(equalTo("L-301")))
                .andExpect(jsonPath("$.msg").value(equalTo("댓글을 찾을 수 없습니다.")))
        }

        @Test
        @DisplayName("여러 사용자가 동일 댓글에 좋아요")
        fun multipleLikesFromDifferentUsers() {
            val users = (0 until 3).map { userHelper.createUser() }
            users.forEach { u ->
                val authorities = AuthorityUtils.createAuthorityList("ROLE_USER")
                val secUser = SecurityUser(u.id!!, u.password!!, u.nickname!!, authorities)
                val auth = UsernamePasswordAuthenticationToken(secUser, null, secUser.authorities)
                SecurityContextHolder.getContext().authentication = auth

                mvc.perform(
                    post(toggleLikeApi, comment.id)
                        .contentType(MediaType.APPLICATION_JSON)
                )
            }
            val likedComment = commentRepository.findById(comment.id!!).orElseThrow()
            assertThat(likedComment.likeCount).isEqualTo(3)
            assertThat(commentLikeRepository.count()).isEqualTo(3)
        }

        @Test
        @DisplayName("좋아요 수가 0 미만으로 감소하지 않음")
        fun likeCountNotBelowZero() {
            assertThat(comment.likeCount).isEqualTo(0)
            mvc.perform(post(toggleLikeApi, comment.id).contentType(MediaType.APPLICATION_JSON)) // 1 (생성)
            mvc.perform(post(toggleLikeApi, comment.id).contentType(MediaType.APPLICATION_JSON)) // 0 (취소)
            mvc.perform(post(toggleLikeApi, comment.id).contentType(MediaType.APPLICATION_JSON)) // 1 (재생성)

            val finalComment = commentRepository.findById(comment.id!!).orElseThrow()
            assertThat(finalComment.likeCount).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("좋아요 중복 방지 및 동시성 테스트")
    inner class ConcurrencyTest {

        @Test
        @DisplayName("동일 사용자의 중복 좋아요 방지 - DB 제약조건")
        fun preventDuplicateLikesBySameUser() {
            val like1 = CommentLike.create(comment, user)
            commentLikeRepository.save(like1)
            val like2 = CommentLike.create(comment, user)
            assertThatThrownBy {
                commentLikeRepository.save(like2)
                commentLikeRepository.flush()
            }.isInstanceOf(Exception::class.java)
        }

        @Test
        @DisplayName("동시 좋아요 요청 처리 - 낙관적 락 적용")
        fun handleConcurrentLikeRequests() {
            val threadCount = 10
            val executorService: ExecutorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            repeat(threadCount) {
                executorService.submit {
                    try {
                        commentLikeService.toggleCommentLikeByCommentId(comment.id!!, user.id!!)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            executorService.shutdown()

            val finalComment = commentRepository.findById(comment.id!!).orElseThrow()
            val expectedLikeState = (successCount.get() % 2) == 1
            val expectedLikeCount = if (expectedLikeState) 1 else 0

            assertThat(finalComment.likeCount).isEqualTo(expectedLikeCount)
            assertThat(commentLikeRepository.existsByCommentAndUser(finalComment, user))
                .isEqualTo(expectedLikeState)
        }
    }

    @Nested
    @DisplayName("댓글 조회 시 좋아요 정보 표시")
    inner class CommentListWithLikeInfo {

        @Test
        @DisplayName("댓글 목록 조회 시 좋아요 정보 포함")
        fun getCommentsWithLikeInfo() {
            val testUser = userHelper.createUser()
            val testRoom = roomHelper.createChallengeRoom(testUser)
            participantHelper.createNormalParticipant(testUser, testRoom)
            val comment1 = commentHelper.createComment(testUser, testRoom, "첫 번째 댓글")
            val comment2 = commentHelper.createComment(testUser, testRoom, "두 번째 댓글")
            val comment3 = commentHelper.createComment(testUser, testRoom, "세 번째 댓글")

            // comment1: testUser가 좋아요
            val like = CommentLike.create(comment1, testUser)
            commentLikeRepository.save(like)
            comment1.incrementLikeCount()
            commentRepository.save(comment1)

            // comment2: otherUser가 좋아요
            val otherUser = userHelper.createUser()
            val otherLike = CommentLike.create(comment2, otherUser)
            commentLikeRepository.save(otherLike)
            comment2.incrementLikeCount()
            commentRepository.save(comment2)

            // 로그인 유저는 testUser
            val authorities = AuthorityUtils.createAuthorityList("ROLE_USER")
            val testSecurityUser = SecurityUser(testUser.id!!, testUser.password!!, testUser.nickname!!, authorities)
            val testAuth = UsernamePasswordAuthenticationToken(testSecurityUser, null, testSecurityUser.authorities)
            SecurityContextHolder.getContext().authentication = testAuth

            val result = mvc.perform(
                get(getCommentsApi, testRoom.id)
                    .contentType(MediaType.APPLICATION_JSON)
            ).andReturn()
            val responseJson = result.response.contentAsString

            // ====== comment1 (본인이 누른 좋아요) ======
            val comment1LikeCounts: List<Int> =
                JsonPath.read(responseJson, "$.data.content[?(@.id == ${comment1.id})].likeCount")
            val comment1Liked: List<Boolean> =
                JsonPath.read(responseJson, "$.data.content[?(@.id == ${comment1.id})].liked")

            assertThat(comment1LikeCounts).hasSize(1)
            assertThat(comment1LikeCounts[0]).isEqualTo(1)
            assertThat(comment1Liked[0]).isTrue()

            // ====== comment2 (다른 사람이 누른 좋아요) ======
            val comment2LikeCounts: List<Int> =
                JsonPath.read(responseJson, "$.data.content[?(@.id == ${comment2.id})].likeCount")
            val comment2Liked: List<Boolean> =
                JsonPath.read(responseJson, "$.data.content[?(@.id == ${comment2.id})].liked")

            assertThat(comment2LikeCounts).hasSize(1)
            assertThat(comment2LikeCounts[0]).isEqualTo(1)
            assertThat(comment2Liked[0]).isFalse()

            // ====== comment3 (좋아요 없음) ======
            val comment3LikeCounts: List<Int> =
                JsonPath.read(responseJson, "$.data.content[?(@.id == ${comment3.id})].likeCount")
            val comment3Liked: List<Boolean> =
                JsonPath.read(responseJson, "$.data.content[?(@.id == ${comment3.id})].liked")

            assertThat(comment3LikeCounts).hasSize(1)
            assertThat(comment3LikeCounts[0]).isEqualTo(0)
            assertThat(comment3Liked[0]).isFalse()
        }
    }

    @Nested
    @DisplayName("댓글 삭제 시 좋아요 처리")
    inner class DeleteCommentWithLikes {
        @Test
        @DisplayName("댓글 삭제 시 관련 좋아요도 모두 삭제")
        fun deleteLikesWhenCommentDeleted() {
            val testUser = userHelper.createUser()
            val testRoom = roomHelper.createChallengeRoom(testUser)
            participantHelper.createNormalParticipant(testUser, testRoom)
            val testComment = commentHelper.createComment(testUser, testRoom, "삭제될 댓글")
            val user1 = userHelper.createUser()
            val user2 = userHelper.createUser()

            val like1 = CommentLike.create(testComment, testUser)
            val like2 = CommentLike.create(testComment, user1)
            val like3 = CommentLike.create(testComment, user2)
            commentLikeRepository.saveAll(listOf(like1, like2, like3))
            repeat(3) { testComment.incrementLikeCount() }
            commentRepository.save(testComment)

            val likesForThisComment = commentLikeRepository.findAll().count { it.comment.id == testComment.id }
            assertThat(likesForThisComment).isEqualTo(3)

            val authorities = AuthorityUtils.createAuthorityList("ROLE_USER")
            val testSecurityUser = SecurityUser(testUser.id!!, testUser.password!!, testUser.nickname!!, authorities)
            val testAuth = UsernamePasswordAuthenticationToken(testSecurityUser, null, testSecurityUser.authorities)
            SecurityContextHolder.getContext().authentication = testAuth

            mvc.perform(delete(deleteCommentApi, testRoom.id, testComment.id).contentType(MediaType.APPLICATION_JSON))

            val likesAfterDelete = commentLikeRepository.findAll().count { it.comment.id == testComment.id }
            assertThat(likesAfterDelete).isEqualTo(0)

            val deletedComment = commentRepository.findById(testComment.id!!).orElseThrow()
            assertThat(deletedComment.isDeleted!!).isTrue()
        }
    }

    @AfterEach
    fun clear() {
        SecurityContextHolder.clearContext()
    }
}
