package com.back.motionit.domain.challenge.like.controller

import com.back.motionit.domain.challenge.comment.entity.Comment
import com.back.motionit.domain.challenge.comment.repository.CommentRepository
import com.back.motionit.domain.challenge.like.entity.CommentLike
import com.back.motionit.domain.challenge.like.repository.CommentLikeRepository
import com.back.motionit.domain.challenge.like.service.CommentLikeService
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.user.entity.User
import com.back.motionit.global.error.code.CommentLikeErrorCode
import com.back.motionit.helper.ChallengeParticipantHelper
import com.back.motionit.helper.ChallengeRoomHelper
import com.back.motionit.helper.CommentHelper
import com.back.motionit.helper.UserHelper
import com.back.motionit.security.SecurityUser
import com.back.motionit.support.BaseIntegrationTest
import com.back.motionit.support.SecuredIntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
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

    private val mapper = ObjectMapper()
    private lateinit var user: User
    private lateinit var room: ChallengeRoom
    private lateinit var comment: Comment
    private lateinit var securityUser: SecurityUser
    private lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setUp() {
        user = userHelper.createUser()
        room = roomHelper.createChallengeRoom(user)
        comment = commentHelper.createComment(user, room, "테스트 댓글입니다.")

        val authorities = AuthorityUtils.createAuthorityList("ROLE_USER")
        securityUser = SecurityUser(user.id!!, user.password, user.nickname, authorities)
        authentication = UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Nested
    @DisplayName("POST `/api/v1/comments/{commentId}/likes` - 댓글 좋아요 토글")
    inner class ToggleCommentLikeTest {

        private val toggleLikeApi = "/api/v1/comments/{commentId}/likes"

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
            // GIVEN: 좋아요 생성 시, 헬퍼 메서드 사용
            val like = CommentLike.create(comment, user)
            commentLikeRepository.save(like)
            comment.incrementLikeCount()
            commentRepository.save(comment)

            // WHEN: 좋아요 취소 요청
            mvc.perform(
                post(toggleLikeApi, comment.id)
                    .contentType(MediaType.APPLICATION_JSON)
            )

            // THEN: 좋아요 카운트가 0이고 좋아요 기록이 없음
            val updatedComment = commentRepository.findById(comment.id!!).orElseThrow()
            assertThat(updatedComment.likeCount).isEqualTo(0)
            assertThat(commentLikeRepository.existsByCommentAndUser(updatedComment, user)).isFalse()
        }

        @Test
        @DisplayName("좋아요 토글 여러 번 수행")
        fun toggleLikeMultipleTimes() {
            mvc.perform(post(toggleLikeApi, comment.id).contentType(MediaType.APPLICATION_JSON)) // 1 (생성)
            mvc.perform(post(toggleLikeApi, comment.id).contentType(MediaType.APPLICATION_JSON)) // 0 (취소)
            mvc.perform(post(toggleLikeApi, comment.id).contentType(MediaType.APPLICATION_JSON)) // 1 (재생성)
            val finalComment = commentRepository.findById(comment.id!!).orElseThrow()
            assertThat(finalComment.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("존재하지 않는 댓글에 좋아요 시도 - 실패")
        fun failedLikeNonExistentComment() {
            val nonExistentCommentId = comment.id!! + 999L
            // Service 계층에서 BusinessException을 던지도록 구현되었다고 가정하고 실패를 검증해야 합니다.
            mvc.perform(
                post(toggleLikeApi, nonExistentCommentId)
                    .contentType(MediaType.APPLICATION_JSON)
            ).andExpect {
                // TODO: 실제 API 오류 응답 구조에 맞게 상태 코드나 오류 메시지 검증 로직 추가 필요
                // status().isNotFound() 혹은 status().isBadRequest() 등
            }
        }

        @Test
        @DisplayName("여러 사용자가 동일 댓글에 좋아요")
        fun multipleLikesFromDifferentUsers() {
            val users = (0 until 3).map { userHelper.createUser() }
            users.forEach { u ->
                val authorities = AuthorityUtils.createAuthorityList("ROLE_USER")
                val secUser = SecurityUser(u.id!!, u.password, u.nickname, authorities)
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
            // 좋아요가 없는 상태에서 취소를 시도해도 에러 없이 0 유지
            mvc.perform(post(toggleLikeApi, comment.id).contentType(MediaType.APPLICATION_JSON)) // 1 (재생성) -> 0 (취소 시도)

            val finalComment = commentRepository.findById(comment.id!!).orElseThrow()
            // 최종 결과는 1(생성) -> 0(취소) -> 1(재생성)
            assertThat(finalComment.likeCount).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("좋아요 중복 방지 및 동시성 테스트")
    inner class ConcurrencyTest {

        @Test
        @DisplayName("동일 사용자의 중복 좋아요 방지 - DB 제약조건")
        fun preventDuplicateLikesBySameUser() {
            // GIVEN: 좋아요 생성 시, 헬퍼 메서드 사용
            val like1 = CommentLike.create(comment, user)
            commentLikeRepository.save(like1)
            // WHEN & THEN: 동일 사용자가 좋아요를 한 번 더 시도하면 DataIntegrityViolationException이 발생해야 함
            val like2 = CommentLike.create(comment, user)
            assertThatThrownBy {
                commentLikeRepository.save(like2)
                commentLikeRepository.flush()
            }.isInstanceOf(Exception::class.java) // DataIntegrityViolationException 또는 그 Wrapper
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
                    // 모든 스레드가 동일한 사용자로 요청
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

            // 최종 상태 검증: 모든 토글 요청이 순차적으로 적용되어야 하며, 최종 좋아요 수는 0 또는 1이 되어야 함
            val finalComment = commentRepository.findById(comment.id!!).orElseThrow()
            // 요청 횟수가 짝수이면 취소 상태 (0), 홀수이면 좋아요 상태 (1)여야 함
            val actualSuccessfulToggles = successCount.get()
            val expectedLikeState = (actualSuccessfulToggles % 2) == 1
            val expectedLikeCount = if (expectedLikeState) 1 else 0

            assertThat(finalComment.likeCount).isEqualTo(expectedLikeCount)
            assertThat(commentLikeRepository.existsByCommentAndUser(finalComment, user))
                .isEqualTo(expectedLikeState)
        }
    }

    @Nested
    @DisplayName("댓글 조회 시 좋아요 정보 표시")
    inner class CommentListWithLikeInfo {
        private val getCommentsApi = "/api/v1/rooms/{roomId}/comments"

        @Test
        @DisplayName("댓글 목록 조회 시 좋아요 정보 포함")
        fun getCommentsWithLikeInfo() {
            val testUser = userHelper.createUser()
            val testRoom = roomHelper.createChallengeRoom(testUser)
            participantHelper.createNormalParticipant(testUser, testRoom)
            val comment1 = commentHelper.createComment(testUser, testRoom, "첫 번째 댓글")
            val comment2 = commentHelper.createComment(testUser, testRoom, "두 번째 댓글")
            val comment3 = commentHelper.createComment(testUser, testRoom, "세 번째 댓글")

            // comment1에만 현재 사용자가 좋아요 (CommentLike.create 사용)
            val like = CommentLike.create(comment1, testUser)
            commentLikeRepository.save(like)
            comment1.incrementLikeCount()
            commentRepository.save(comment1)

            // comment2에는 다른 사용자가 좋아요 (CommentLike.create 사용)
            val otherUser = userHelper.createUser()
            val otherLike = CommentLike.create(comment2, otherUser)
            commentLikeRepository.save(otherLike)
            comment2.incrementLikeCount()
            commentRepository.save(comment2)

            val authorities = AuthorityUtils.createAuthorityList("ROLE_USER")
            val testSecurityUser = SecurityUser(testUser.id!!, testUser.password, testUser.nickname, authorities)
            val testAuth = UsernamePasswordAuthenticationToken(testSecurityUser, null, testSecurityUser.authorities)
            SecurityContextHolder.getContext().authentication = testAuth

            val result = mvc.perform(get(getCommentsApi, testRoom.id).contentType(MediaType.APPLICATION_JSON)).andReturn()
            val responseJson = result.response.contentAsString

            // 검증: comment1 (좋아요 수 1, isLiked: true)
            val comment1LikeCounts: List<Int> = JsonPath.read(responseJson, "$.data.content[?(@.id == ${comment1.id})].likeCount")
            val comment1IsLiked: List<Boolean> = JsonPath.read(responseJson, "$.data.content[?(@.id == ${comment1.id})].isLiked")
            assertThat(comment1LikeCounts).hasSize(1)
            assertThat(comment1LikeCounts[0]).isEqualTo(1)
            assertThat(comment1IsLiked[0]).isTrue()

            // 검증: comment2 (좋아요 수 1, isLiked: false - 다른 사용자가 좋아요 했으므로)
            val comment2LikeCounts: List<Int> = JsonPath.read(responseJson, "$.data.content[?(@.id == ${comment2.id})].likeCount")
            val comment2IsLiked: List<Boolean> = JsonPath.read(responseJson, "$.data.content[?(@.id == ${comment2.id})].isLiked")
            assertThat(comment2LikeCounts).hasSize(1)
            assertThat(comment2LikeCounts[0]).isEqualTo(1)
            assertThat(comment2IsLiked[0]).isFalse()

            // 검증: comment3 (좋아요 수 0, isLiked: false)
            val comment3LikeCounts: List<Int> = JsonPath.read(responseJson, "$.data.content[?(@.id == ${comment3.id})].likeCount")
            val comment3IsLiked: List<Boolean> = JsonPath.read(responseJson, "$.data.content[?(@.id == ${comment3.id})].isLiked")
            assertThat(comment3LikeCounts).hasSize(1)
            assertThat(comment3LikeCounts[0]).isEqualTo(0)
            assertThat(comment3IsLiked[0]).isFalse()
        }
    }

    @Nested
    @DisplayName("댓글 삭제 시 좋아요 처리")
    inner class DeleteCommentWithLikes {
        private val deleteCommentApi = "/api/v1/rooms/{roomId}/comments/{commentId}"

        @Test
        @DisplayName("댓글 삭제 시 관련 좋아요도 모두 삭제")
        fun deleteLikesWhenCommentDeleted() {
            val testUser = userHelper.createUser()
            val testRoom = roomHelper.createChallengeRoom(testUser)
            participantHelper.createNormalParticipant(testUser, testRoom)
            val testComment = commentHelper.createComment(testUser, testRoom, "삭제될 댓글")
            val user1 = userHelper.createUser()
            val user2 = userHelper.createUser()

            // 좋아요 생성 시 CommentLike.create 사용
            val like1 = CommentLike.create(testComment, testUser)
            val like2 = CommentLike.create(testComment, user1)
            val like3 = CommentLike.create(testComment, user2)
            commentLikeRepository.saveAll(listOf(like1, like2, like3))
            repeat(3) { testComment.incrementLikeCount() }
            commentRepository.save(testComment)

            val likesForThisComment = commentLikeRepository.findAll().count { it.comment.id == testComment.id }
            assertThat(likesForThisComment).isEqualTo(3)

            val authorities = AuthorityUtils.createAuthorityList("ROLE_USER")
            val testSecurityUser = SecurityUser(testUser.id!!, testUser.password, testUser.nickname, authorities)
            val testAuth = UsernamePasswordAuthenticationToken(testSecurityUser, null, testSecurityUser.authorities)
            SecurityContextHolder.getContext().authentication = testAuth

            // WHEN: 댓글 삭제 요청 (CommentService에서 deleteAllByComment가 호출될 것으로 가정)
            mvc.perform(delete(deleteCommentApi, testRoom.id, testComment.id).contentType(MediaType.APPLICATION_JSON))

            // THEN: 관련 좋아요 모두 삭제 확인
            val likesAfterDelete = commentLikeRepository.findAll().count { it.comment.id == testComment.id }
            assertThat(likesAfterDelete).isEqualTo(0)

            // THEN: 댓글은 soft-delete 되었는지 확인
            val deletedComment = commentRepository.findById(testComment.id!!).orElseThrow()
            assertThat(deletedComment.isDeleted).isTrue()
        }
    }

    @AfterEach
    fun clear() {
        SecurityContextHolder.clearContext()
    }
}