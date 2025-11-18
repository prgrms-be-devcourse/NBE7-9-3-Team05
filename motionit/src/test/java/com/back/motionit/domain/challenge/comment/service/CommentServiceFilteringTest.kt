package com.back.motionit.domain.challenge.comment.service

import com.back.motionit.domain.challenge.comment.dto.CommentCreateReq
import com.back.motionit.domain.challenge.comment.dto.CommentEditReq
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
import com.back.motionit.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class CommentServiceFilteringTest {

    @Mock
    private lateinit var commentRepository: CommentRepository

    @Mock
    private lateinit var challengeRoomRepository: ChallengeRoomRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    lateinit var commentLikeService: CommentLikeService

    @Mock
    private lateinit var commentLikeRepository: CommentLikeRepository

    @Mock
    private lateinit var commentModeration: CommentModeration

    @Mock
    private lateinit var challengeAuthValidator: ChallengeAuthValidator

    @InjectMocks
    private lateinit var commentService: CommentService

    private lateinit var mockRoom: ChallengeRoom
    private lateinit var author: User

    @BeforeEach
    fun setUp() {
        mockRoom = mock(ChallengeRoom::class.java)
        author = User(USER_ID, "alice")   // 기존 자바 테스트와 동일 가정

        // lenient: 방 id 조회가 안 쓰이는 테스트에서도 stubbing 예외 안 나게
        lenient().`when`(mockRoom.id).thenReturn(ROOM_ID)
    }

    // 활성 댓글 빌더 (id 주입)
    private fun activeComment(
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

    // -------- 공통 가드 스텁 (CREATE용): 불필요 스텁 최소화 ----------
    private fun stubGuardsForCreateBasic() {
        `when`(challengeRoomRepository.existsById(ROOM_ID)).thenReturn(true)
        `when`(challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID))
            .thenReturn(null as ChallengeParticipant?)

        @Suppress("UNCHECKED_CAST")
        val optionalRoom: Optional<ChallengeRoom?> =
            Optional.of(mockRoom) as Optional<ChallengeRoom?>

        `when`(challengeRoomRepository.findById(ROOM_ID))
            .thenReturn(optionalRoom)

        `when`(userRepository.findById(USER_ID))
            .thenReturn(Optional.of(author))
    }

    // -------- 공통 가드 스텁 (EDIT용): 불필요 스텁 최소화 ----------
    private fun stubGuardsForEditBasic(target: Comment) {
        `when`(challengeRoomRepository.existsById(ROOM_ID)).thenReturn(true)
        `when`(challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID))
            .thenReturn(null as ChallengeParticipant?)
        `when`(
            commentRepository.findByIdAndChallengeRoom_IdAndDeletedAtIsNull(
                COMMENT_ID,
                ROOM_ID
            )
        ).thenReturn(Optional.of(target))
        // like 조회는 ALLOW 성공 케이스에서만 사용 → 각 테스트에서 개별 스텁
    }

    // ------------------------ CREATE (필터링) ------------------------

    @Test
    @DisplayName("create: ALLOW → 저장 성공")
    fun create_allow_saves() {
        stubGuardsForCreateBasic()
        // ALLOW
        doNothing().`when`(commentModeration).assertClean("정상 댓글")

        // save 시 id 부여
        `when`(commentRepository.save(any(Comment::class.java))).thenAnswer { inv ->
            val saved = inv.getArgument<Comment>(0)
            val now = LocalDateTime.now()
            ReflectionTestUtils.setField(saved, "id", 777L)
            ReflectionTestUtils.setField(saved, "createDate", now)
            ReflectionTestUtils.setField(saved, "modifyDate", now)
            saved
        }

        val res = commentService.create(ROOM_ID, USER_ID, CommentCreateReq("정상 댓글"))

        assertThat(res).isNotNull()
        assertThat(res.roomId).isEqualTo(ROOM_ID)
        assertThat(res.authorId).isEqualTo(USER_ID)
        verify(commentRepository, times(1)).save(any(Comment::class.java))
        verify(commentModeration, times(1)).assertClean("정상 댓글")
    }

    @Test
    @DisplayName("create: WARN → INAPPROPRIATE_CONTENT_WARN 예외")
    fun create_warn_throws() {
        stubGuardsForCreateBasic()
        doThrow(BusinessException(CommentErrorCode.INAPPROPRIATE_CONTENT_WARN))
            .`when`(commentModeration).assertClean("살짝 비매너")

        val ex = assertThrows(BusinessException::class.java) {
            commentService.create(ROOM_ID, USER_ID, CommentCreateReq("살짝 비매너"))
        }

        assertThat(ex.errorCode).isEqualTo(CommentErrorCode.INAPPROPRIATE_CONTENT_WARN)
        verifyNoInteractions(commentLikeRepository)
    }

    @Test
    @DisplayName("create: BLOCK → INAPPROPRIATE_CONTENT_BLOCK 예외")
    fun create_block_throws() {
        stubGuardsForCreateBasic()
        doThrow(BusinessException(CommentErrorCode.INAPPROPRIATE_CONTENT_BLOCK))
            .`when`(commentModeration).assertClean("심한 욕설")

        val ex = assertThrows(BusinessException::class.java) {
            commentService.create(ROOM_ID, USER_ID, CommentCreateReq("심한 욕설"))
        }

        assertThat(ex.errorCode).isEqualTo(CommentErrorCode.INAPPROPRIATE_CONTENT_BLOCK)
        verifyNoInteractions(commentLikeRepository)
    }

    // ------------------------ EDIT (필터링) ------------------------

    @Test
    @DisplayName("edit: ALLOW → 내용 업데이트 성공")
    fun edit_allow_updates() {
        val own = activeComment(COMMENT_ID, mockRoom, author, "old")
        stubGuardsForEditBasic(own)

        // ALLOW
        doNothing().`when`(commentModeration).assertClean("new clean")

        // 좋아요 여부/참조
        `when`(userRepository.getReferenceById(USER_ID)).thenReturn(author)
        `when`(commentLikeRepository.existsByCommentAndUser(own, author)).thenReturn(false)

        val res = commentService.edit(ROOM_ID, COMMENT_ID, USER_ID, CommentEditReq("new clean"))

        assertThat(res).isNotNull()
        verify(commentModeration, times(1)).assertClean("new clean")
        verify(commentLikeRepository, times(1)).existsByCommentAndUser(own, author)
    }

    @Test
    @DisplayName("edit: WARN → INAPPROPRIATE_CONTENT_WARN 예외")
    fun edit_warn_throws() {
        val own = activeComment(COMMENT_ID, mockRoom, author, "old")
        stubGuardsForEditBasic(own)

        doThrow(BusinessException(CommentErrorCode.INAPPROPRIATE_CONTENT_WARN))
            .`when`(commentModeration).assertClean("살짝 비매너")

        val ex = assertThrows(BusinessException::class.java) {
            commentService.edit(ROOM_ID, COMMENT_ID, USER_ID, CommentEditReq("살짝 비매너"))
        }

        assertThat(ex.errorCode).isEqualTo(CommentErrorCode.INAPPROPRIATE_CONTENT_WARN)


        // ✅ 이렇게 "단독 호출"만 남겨
        verifyNoInteractions(commentLikeRepository)

        verifyNoInteractions(commentLikeService)
    }

    @Test
    @DisplayName("edit: BLOCK → INAPPROPRIATE_CONTENT_BLOCK 예외")
    fun edit_block_throws() {
        val own = activeComment(COMMENT_ID, mockRoom, author, "old")
        stubGuardsForEditBasic(own)

        doThrow(BusinessException(CommentErrorCode.INAPPROPRIATE_CONTENT_BLOCK))
            .`when`(commentModeration).assertClean("심한 욕설")

        val ex = assertThrows(BusinessException::class.java) {
            commentService.edit(ROOM_ID, COMMENT_ID, USER_ID, CommentEditReq("심한 욕설"))
        }

        assertThat(ex.errorCode).isEqualTo(CommentErrorCode.INAPPROPRIATE_CONTENT_BLOCK)

        // ❌ 절대 이렇게 쓰면 안 됨:
        // verify(commentLikeRepository, never())
        //     .existsByCommentAndUser(any(), any())

        // ✅ 이렇게만!
        verifyNoInteractions(commentLikeRepository)
        verifyNoInteractions(commentLikeService)
    }

    companion object {
        private const val ROOM_ID = 10L
        private const val USER_ID = 101L
        private const val COMMENT_ID = 1000L
    }
}