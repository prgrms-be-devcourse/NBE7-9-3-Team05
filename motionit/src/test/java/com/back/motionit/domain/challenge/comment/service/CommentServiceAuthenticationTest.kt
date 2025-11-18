package com.back.motionit.domain.challenge.comment.service

import com.back.motionit.domain.challenge.comment.dto.CommentCreateReq
import com.back.motionit.domain.challenge.comment.dto.CommentEditReq
import com.back.motionit.domain.challenge.comment.dto.CommentRes
import com.back.motionit.domain.challenge.comment.entity.Comment
import com.back.motionit.domain.challenge.comment.moderation.CommentModeration
import com.back.motionit.domain.challenge.comment.repository.CommentRepository
import com.back.motionit.domain.challenge.like.repository.CommentLikeRepository
import com.back.motionit.domain.challenge.like.service.CommentLikeService
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.challenge.validator.ChallengeAuthValidator
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.error.code.CommentErrorCode
import com.back.motionit.global.error.code.ErrorCode
import com.back.motionit.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class CommentServiceAuthenticationTest {

    @Mock
    lateinit var commentRepository: CommentRepository

    @Mock
    lateinit var challengeRoomRepository: ChallengeRoomRepository

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var commentLikeRepository: CommentLikeRepository

    @Mock
    lateinit var commentLikeService: CommentLikeService   // ✅ 새로 추가된 서비스 모킹

    @Mock
    lateinit var commentModeration: CommentModeration

    @Mock
    lateinit var challengeAuthValidator: ChallengeAuthValidator

    @InjectMocks
    lateinit var commentService: CommentService

    // 테스트 전역에서 사용할 mock/fixture
    private lateinit var mockRoom: ChallengeRoom
    private lateinit var author: User
    private lateinit var otherUser: User

    @BeforeEach
    fun setUp() {
        mockRoom = Mockito.mock(ChallengeRoom::class.java)
        author = User(USER_ID, "alice")
        otherUser = User(OTHER_USER_ID, "bob")

        // 필요 테스트에서 호출되지 않아도 불필요 stubbing 예외가 나지 않도록 lenient 사용
        Mockito.lenient()
            .`when`(mockRoom.id)
            .thenReturn(ROOM_ID)
    }

    // Comment 엔티티 id + 날짜 주입 유틸 (Reflection)
    private fun buildActiveComment(
        id: Long,
        room: ChallengeRoom,
        user: User,
        content: String,
    ): Comment {
        val comment = Comment(
            deletedAt = null,
            challengeRoom = room,
            user = user,
            content = content,
            likeCount = 0,
            version = null,
        )

        val now = LocalDateTime.now()
        ReflectionTestUtils.setField(comment, "id", id)
        ReflectionTestUtils.setField(comment, "createDate", now)
        ReflectionTestUtils.setField(comment, "modifyDate", now)

        return comment
    }

    @Nested
    @DisplayName("create()")
    inner class CreateTests {

        @Test
        @DisplayName("방 존재 + 멤버십 OK → 댓글 생성 성공")
        fun create_success() {
            // guard: 방 존재
            Mockito.`when`(challengeRoomRepository.existsById(ROOM_ID))
                .thenReturn(true)

            // 방, 사용자 조회
            Mockito.`when`(challengeRoomRepository.findById(ROOM_ID))
                .thenReturn(Optional.of(mockRoom))

            Mockito.`when`(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(author))

            // 멤버십 통과 (반환값은 사용하지 않으므로 아무 Participant mock이면 됨)
            Mockito.`when`(
                challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)
            ).thenReturn(Mockito.mock(ChallengeParticipant::class.java))

            // 모더레이션 통과
            Mockito.doNothing()
                .`when`(commentModeration)
                .assertClean("hello")

            // save 시 id + 날짜 부여
            Mockito.`when`(commentRepository.save(Mockito.any(Comment::class.java))).thenAnswer { inv ->
                val saved = inv.getArgument<Comment>(0)
                val now = LocalDateTime.now()
                ReflectionTestUtils.setField(saved, "id", 777L)
                ReflectionTestUtils.setField(saved, "createDate", now)
                ReflectionTestUtils.setField(saved, "modifyDate", now)
                saved
            }

            val res = commentService.create(ROOM_ID, USER_ID, CommentCreateReq("hello"))

            assertThat(res).isNotNull()
            assertThat(res.roomId).isEqualTo(ROOM_ID)
            assertThat(res.authorId).isEqualTo(USER_ID)

            Mockito.verify(commentRepository, Mockito.times(1))
                .save(Mockito.any(Comment::class.java))
            Mockito.verify(commentModeration, Mockito.times(1))
                .assertClean("hello")
            Mockito.verify(challengeAuthValidator, Mockito.times(1))
                .validateActiveParticipant(USER_ID, ROOM_ID)
        }

        @Test
        @DisplayName("방 없음 → ROOM_NOT_FOUND")
        fun create_roomNotFound() {
            Mockito.`when`(challengeRoomRepository.existsById(ROOM_ID))
                .thenReturn(false)

            val ex = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException::class.java,
                Executable {
                    commentService.create(ROOM_ID, USER_ID, CommentCreateReq("hi"))
                }
            )

            assertThat<ErrorCode>(ex.errorCode)
                .isEqualTo(CommentErrorCode.ROOM_NOT_FOUND)

            Mockito.verify(challengeRoomRepository, Mockito.times(1))
                .existsById(ROOM_ID)
            Mockito.verifyNoMoreInteractions(
                commentRepository,
                challengeAuthValidator,
                commentModeration
            )
        }
    }

    @Nested
    @DisplayName("list()")
    inner class ListTests {

        @Test
        @DisplayName("활성 댓글 없음 → 빈 페이지")
        fun list_empty() {
            Mockito.`when`(challengeRoomRepository.existsById(ROOM_ID))
                .thenReturn(true)

            Mockito.`when`(
                challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)
            ).thenReturn(Mockito.mock(ChallengeParticipant::class.java))

            Mockito.`when`(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(author))

            val pageable: Pageable = PageRequest.of(0, 20)

            Mockito.`when`(
                commentRepository.findActiveByRoomIdWithAuthor(
                    ROOM_ID,
                    pageable
                )
            ).thenReturn(Page.empty(pageable))

            val page: Page<CommentRes> = commentService.list(ROOM_ID, USER_ID, 0, 20)

            assertThat(page.totalElements).isZero()
            // 좋아요 조회는 아예 호출되지 않아야 함 → 이제 Service 기준
            Mockito.verifyNoInteractions(commentLikeService)
        }

        @Test
        @DisplayName("활성 댓글 + 좋아요 반영")
        fun list_withLikes() {
            Mockito.`when`(challengeRoomRepository.existsById(ROOM_ID))
                .thenReturn(true)

            Mockito.`when`(
                challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)
            ).thenReturn(Mockito.mock(ChallengeParticipant::class.java))

            Mockito.`when`(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(author))

            val c1 = buildActiveComment(COMMENT_ID, mockRoom, author, "c1")
            val c2 = buildActiveComment(COMMENT_ID + 1, mockRoom, author, "c2")

            val pageable: Pageable = PageRequest.of(0, 20)

            Mockito.`when`(
                commentRepository.findActiveByRoomIdWithAuthor(
                    ROOM_ID,
                    pageable
                )
            ).thenReturn(PageImpl(listOf(c1, c2), pageable, 2))

            val expectedIds = listOf(COMMENT_ID, COMMENT_ID + 1)

            // (C) 실제 호출 인자와 정확히 일치하도록 스텁 → 이제 CommentLikeService 기준
            Mockito.`when`(
                commentLikeService.findLikedCommentIdsSafely(
                    author,
                    expectedIds
                )
            ).thenReturn(setOf(COMMENT_ID))

            val page: Page<CommentRes> = commentService.list(ROOM_ID, USER_ID, 0, 20)

            assertThat(page.totalElements).isEqualTo(2)

            Mockito.verify(commentLikeService, Mockito.times(1))
                .findLikedCommentIdsSafely(author, expectedIds)

            // 선택: liked 플래그까지 검증
            assertThat(page.content[0].liked).isTrue()
            assertThat(page.content[1].liked).isFalse()
        }
    }

    @Nested
    @DisplayName("edit()")
    inner class EditTests {

        @Test
        @DisplayName("작성자 아님 → WRONG_ACCESS")
        fun edit_wrongAccess() {
            Mockito.`when`(challengeRoomRepository.existsById(ROOM_ID))
                .thenReturn(true)

            Mockito.`when`(
                challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)
            ).thenReturn(Mockito.mock(ChallengeParticipant::class.java))

            val othersComment = buildActiveComment(COMMENT_ID, mockRoom, otherUser, "old")

            Mockito.`when`(
                commentRepository.findByIdAndChallengeRoom_IdAndDeletedAtIsNull(
                    COMMENT_ID,
                    ROOM_ID
                )
            ).thenReturn(Optional.of(othersComment))

            val ex = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException::class.java,
                Executable {
                    commentService.edit(
                        ROOM_ID,
                        COMMENT_ID,
                        USER_ID,
                        CommentEditReq("new")
                    )
                }
            )

            assertThat<ErrorCode>(ex.errorCode)
                .isEqualTo(CommentErrorCode.WRONG_ACCESS)

            // 모더레이션은 호출되지 않아야 함
            Mockito.verifyNoInteractions(commentModeration)
        }

        @Test
        @DisplayName("성공 → 내용 변경 및 좋아요 여부 포함")
        fun edit_success() {
            Mockito.`when`(challengeRoomRepository.existsById(ROOM_ID))
                .thenReturn(true)

            Mockito.`when`(
                challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)
            ).thenReturn(Mockito.mock(ChallengeParticipant::class.java))

            val own = buildActiveComment(COMMENT_ID, mockRoom, author, "old")

            Mockito.`when`(
                commentRepository.findByIdAndChallengeRoom_IdAndDeletedAtIsNull(
                    COMMENT_ID,
                    ROOM_ID
                )
            ).thenReturn(Optional.of(own))

            Mockito.doNothing()
                .`when`(commentModeration)
                .assertClean("new")

            Mockito.`when`(userRepository.getReferenceById(USER_ID))
                .thenReturn(author)

            Mockito.`when`(commentLikeRepository.existsByCommentAndUser(own, author))
                .thenReturn(true)

            val res = commentService.edit(
                ROOM_ID,
                COMMENT_ID,
                USER_ID,
                CommentEditReq("new")
            )

            assertThat(res).isNotNull
            Mockito.verify(commentModeration, Mockito.times(1))
                .assertClean("new")
            Mockito.verify(commentLikeRepository, Mockito.times(1))
                .existsByCommentAndUser(own, author)
        }
    }

    @Nested
    @DisplayName("delete()")
    inner class DeleteTests {

        @Test
        @DisplayName("성공 → soft delete 및 좋아요 삭제 호출")
        fun delete_success() {
            Mockito.`when`(challengeRoomRepository.existsById(ROOM_ID))
                .thenReturn(true)

            Mockito.`when`(
                challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)
            ).thenReturn(Mockito.mock(ChallengeParticipant::class.java))

            val own = buildActiveComment(COMMENT_ID, mockRoom, author, "bye")

            Mockito.`when`(
                commentRepository.findByIdAndChallengeRoom_IdAndDeletedAtIsNull(
                    COMMENT_ID,
                    ROOM_ID
                )
            ).thenReturn(Optional.of(own))

            Mockito.doNothing()
                .`when`(commentLikeRepository)
                .deleteAllByComment(own)

            val res = commentService.delete(ROOM_ID, COMMENT_ID, USER_ID)

            assertThat(res).isNotNull
            Mockito.verify(commentLikeRepository, Mockito.times(1))
                .deleteAllByComment(own)
        }

        @Test
        @DisplayName("댓글 없음 → COMMENT_NOT_FOUND")
        fun delete_commentNotFound() {
            Mockito.`when`(challengeRoomRepository.existsById(ROOM_ID))
                .thenReturn(true)

            Mockito.`when`(
                challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)
            ).thenReturn(Mockito.mock(ChallengeParticipant::class.java))

            Mockito.`when`(
                commentRepository.findByIdAndChallengeRoom_IdAndDeletedAtIsNull(
                    COMMENT_ID,
                    ROOM_ID
                )
            ).thenReturn(Optional.empty())

            val ex = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException::class.java,
                Executable {
                    commentService.delete(ROOM_ID, COMMENT_ID, USER_ID)
                }
            )

            assertThat<ErrorCode>(ex.errorCode)
                .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND)

            // 좋아요 삭제는 호출되면 안 됨
            Mockito.verifyNoInteractions(commentLikeRepository)
        }
    }

    companion object {
        private const val ROOM_ID = 10L
        private const val USER_ID = 101L
        private const val OTHER_USER_ID = 202L
        private const val COMMENT_ID = 1000L
    }
}
