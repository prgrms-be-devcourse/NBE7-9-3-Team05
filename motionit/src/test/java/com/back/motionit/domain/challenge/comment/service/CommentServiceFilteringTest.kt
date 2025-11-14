package com.back.motionit.domain.challenge.comment.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.motionit.domain.challenge.comment.dto.CommentCreateReq;
import com.back.motionit.domain.challenge.comment.dto.CommentEditReq;
import com.back.motionit.domain.challenge.comment.dto.CommentRes;
import com.back.motionit.domain.challenge.comment.entity.Comment;
import com.back.motionit.domain.challenge.comment.moderation.CommentModeration;
import com.back.motionit.domain.challenge.comment.repository.CommentRepository;
import com.back.motionit.domain.challenge.like.repository.CommentLikeRepository;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.challenge.validator.ChallengeAuthValidator;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.error.code.CommentErrorCode;
import com.back.motionit.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class CommentServiceFilteringTest {

	@Mock
	private CommentRepository commentRepository;
	@Mock
	private ChallengeRoomRepository challengeRoomRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private CommentLikeRepository commentLikeRepository;
	@Mock
	private CommentModeration commentModeration;
	@Mock
	private ChallengeAuthValidator challengeAuthValidator;

	@InjectMocks
	private CommentService commentService;

	private static final Long ROOM_ID = 10L;
	private static final Long USER_ID = 101L;
	private static final Long COMMENT_ID = 1000L;

	private ChallengeRoom mockRoom;
	private User author;

	@BeforeEach
	void setUp() {
		mockRoom = mock(ChallengeRoom.class);
		author = new User(USER_ID, "alice");
		// 일부 테스트에서 호출되지 않아도 괜찮도록 lenient
		lenient().when(mockRoom.getId()).thenReturn(ROOM_ID);
	}

	// 활성 댓글 빌더 (id 주입)
	private Comment activeComment(Long id, ChallengeRoom room, User user, String content) {
		Comment comment = Comment.builder()
			.challengeRoom(room)
			.user(user)
			.content(content)
			.build();
		ReflectionTestUtils.setField(comment, "id", id);
		return comment;
	}

	// -------- 공통 가드 스텁 (CREATE용): 불필요 스텁 최소화 ----------
	private void stubGuardsForCreateBasic() {
		when(challengeRoomRepository.existsById(ROOM_ID)).thenReturn(true);
		when(challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)).thenReturn(null);
		when(challengeRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(mockRoom));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));
		// save 시 id 부여는 "ALLOW 성공" 케이스에만 필요 → 각 테스트에서 개별로 스텁
	}

	// -------- 공통 가드 스텁 (EDIT용): 불필요 스텁 최소화 ----------
	private void stubGuardsForEditBasic(Comment target) {
		when(challengeRoomRepository.existsById(ROOM_ID)).thenReturn(true);
		when(challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)).thenReturn(null);
		when(commentRepository.findByIdAndChallengeRoom_IdAndDeletedAtIsNull(COMMENT_ID, ROOM_ID))
			.thenReturn(Optional.of(target));
		// like 조회/참조는 "ALLOW 성공" 케이스에서만 필요 → 각 테스트에서 개별로 스텁
	}

	// ------------------------ CREATE (필터링) ------------------------

	@Test
	@DisplayName("create: ALLOW → 저장 성공")
	void create_allow_saves() {
		stubGuardsForCreateBasic();
		// ALLOW
		doNothing().when(commentModeration).assertClean("정상 댓글");

		// save 시 id 부여 (여기서만 필요) — 정확히 객체 인스턴스에 setField
		doAnswer(inv -> {
			Object arg = inv.getArgument(0);
			ReflectionTestUtils.setField(arg, "id", 777L);
			return null;
		}).when(commentRepository).save(any(Comment.class));

		CommentRes res = commentService.create(ROOM_ID, USER_ID, new CommentCreateReq("정상 댓글"));

		assertThat(res).isNotNull();
		assertThat(res.roomId()).isEqualTo(ROOM_ID);
		assertThat(res.authorId()).isEqualTo(USER_ID);
		verify(commentRepository, times(1)).save(any(Comment.class));
		verify(commentModeration, times(1)).assertClean("정상 댓글");
	}

	@Test
	@DisplayName("create: WARN → INAPPROPRIATE_CONTENT_WARN 예외")
	void create_warn_throws() {
		stubGuardsForCreateBasic();
		doThrow(new BusinessException(CommentErrorCode.INAPPROPRIATE_CONTENT_WARN))
			.when(commentModeration).assertClean("살짝 비매너");

		BusinessException ex = assertThrows(
			BusinessException.class,
			() -> commentService.create(ROOM_ID, USER_ID, new CommentCreateReq("살짝 비매너"))
		);

		assertThat(ex.getErrorCode()).isEqualTo(CommentErrorCode.INAPPROPRIATE_CONTENT_WARN);
		verify(commentRepository, never()).save(any());
	}

	@Test
	@DisplayName("create: BLOCK → INAPPROPRIATE_CONTENT_BLOCK 예외")
	void create_block_throws() {
		stubGuardsForCreateBasic();
		doThrow(new BusinessException(CommentErrorCode.INAPPROPRIATE_CONTENT_BLOCK))
			.when(commentModeration).assertClean("심한 욕설");

		BusinessException ex = assertThrows(
			BusinessException.class,
			() -> commentService.create(ROOM_ID, USER_ID, new CommentCreateReq("심한 욕설"))
		);

		assertThat(ex.getErrorCode()).isEqualTo(CommentErrorCode.INAPPROPRIATE_CONTENT_BLOCK);
		verify(commentRepository, never()).save(any());
	}

	// ------------------------ EDIT (필터링) ------------------------

	@Test
	@DisplayName("edit: ALLOW → 내용 업데이트 성공")
	void edit_allow_updates() {
		Comment own = activeComment(COMMENT_ID, mockRoom, author, "old");
		stubGuardsForEditBasic(own);

		// ALLOW
		doNothing().when(commentModeration).assertClean("new clean");

		// 좋아요 여부/참조 — ALLOW 성공 케이스에서만 필요하므로 여기서만 스텁
		when(userRepository.getReferenceById(USER_ID)).thenReturn(author);
		when(commentLikeRepository.existsByCommentAndUser(own, author)).thenReturn(false);

		CommentRes res = commentService.edit(ROOM_ID, COMMENT_ID, USER_ID, new CommentEditReq("new clean"));

		assertThat(res).isNotNull();
		verify(commentModeration, times(1)).assertClean("new clean");
		verify(commentLikeRepository, times(1)).existsByCommentAndUser(own, author);
	}

	@Test
	@DisplayName("edit: WARN → INAPPROPRIATE_CONTENT_WARN 예외")
	void edit_warn_throws() {
		Comment own = activeComment(COMMENT_ID, mockRoom, author, "old");
		stubGuardsForEditBasic(own);

		doThrow(new BusinessException(CommentErrorCode.INAPPROPRIATE_CONTENT_WARN))
			.when(commentModeration).assertClean("살짝 비매너");

		BusinessException ex = assertThrows(
			BusinessException.class,
			() -> commentService.edit(ROOM_ID, COMMENT_ID, USER_ID, new CommentEditReq("살짝 비매너"))
		);
		assertThat(ex.getErrorCode()).isEqualTo(CommentErrorCode.INAPPROPRIATE_CONTENT_WARN);

		// WARN으로 바로 예외 → like 조회 없어야 함
		verify(commentLikeRepository, never()).existsByCommentAndUser(any(), any());
	}

	@Test
	@DisplayName("edit: BLOCK → INAPPROPRIATE_CONTENT_BLOCK 예외")
	void edit_block_throws() {
		Comment own = activeComment(COMMENT_ID, mockRoom, author, "old");
		stubGuardsForEditBasic(own);

		doThrow(new BusinessException(CommentErrorCode.INAPPROPRIATE_CONTENT_BLOCK))
			.when(commentModeration).assertClean("심한 욕설");

		BusinessException ex = assertThrows(
			BusinessException.class,
			() -> commentService.edit(ROOM_ID, COMMENT_ID, USER_ID, new CommentEditReq("심한 욕설"))
		);
		assertThat(ex.getErrorCode()).isEqualTo(CommentErrorCode.INAPPROPRIATE_CONTENT_BLOCK);

		// BLOCK으로 바로 예외 → like 조회 없어야 함
		verify(commentLikeRepository, never()).existsByCommentAndUser(any(), any());
	}
}
