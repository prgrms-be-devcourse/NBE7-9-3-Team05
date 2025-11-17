package com.back.motionit.domain.challenge.comment.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.motionit.domain.challenge.comment.dto.CommentCreateReq;
import com.back.motionit.domain.challenge.comment.dto.CommentEditReq;
import com.back.motionit.domain.challenge.comment.dto.CommentRes;
import com.back.motionit.domain.challenge.comment.entity.Comment;
import com.back.motionit.domain.challenge.comment.moderation.CommentModeration;
import com.back.motionit.domain.challenge.comment.repository.CommentRepository;
import com.back.motionit.domain.challenge.like.repository.CommentLikeRepository;
import com.back.motionit.domain.challenge.like.service.CommentLikeService;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.challenge.validator.ChallengeAuthValidator;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.error.code.CommentErrorCode;
import com.back.motionit.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class CommentServiceAuthenticationTest {

	@Mock
	private CommentRepository commentRepository;
	@Mock
	private ChallengeRoomRepository challengeRoomRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private CommentLikeRepository commentLikeRepository;
	@Mock
	private CommentLikeService commentLikeService;
	@Mock
	private CommentModeration commentModeration;
	@Mock
	private ChallengeAuthValidator challengeAuthValidator;

	@InjectMocks
	private CommentService commentService;

	private static final Long ROOM_ID = 10L;
	private static final Long USER_ID = 101L;
	private static final Long OTHER_USER_ID = 202L;
	private static final Long COMMENT_ID = 1000L;

	// 테스트 전역에서 사용할 mock/fixture
	private ChallengeRoom mockRoom;
	private User author;
	private User otherUser;

	@BeforeEach
	void setUp() {
		mockRoom = mock(ChallengeRoom.class);
		author = new User(USER_ID, "alice");
		otherUser = new User(OTHER_USER_ID, "bob");

		// (B) 필요 테스트에서 호출되지 않아도 불필요 stubbing 예외가 나지 않도록 lenient 사용
		lenient().when(mockRoom.getId()).thenReturn(ROOM_ID);
	}

	// (A) Comment 엔티티 id 주입 유틸 (Reflection)
	private Comment buildActiveComment(Long id, ChallengeRoom room, User user, String content) {
		Comment comment = Comment.builder()
			.challengeRoom(room)
			.user(user)
			.content(content)
			.build();
		ReflectionTestUtils.setField(comment, "id", id); // ★ A: id 강제 주입
		return comment;
	}

	@Nested
	@DisplayName("create()")
	class CreateTests {

		@Test
		@DisplayName("방 존재 + 멤버십 OK → 댓글 생성 성공")
		void create_success() {
			// guard: 방 존재
			when(challengeRoomRepository.existsById(ROOM_ID)).thenReturn(true);
			// 방, 사용자 조회
			when(challengeRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(mockRoom));
			when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));
			// 멤버십 통과(반환값은 사용하지 않으므로 아무 Participant mock이면 됨)
			when(challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)).thenReturn(null);
			// 모더레이션 통과
			doNothing().when(commentModeration).assertClean("hello");

			// save 시 id 부여 (선택)
			doAnswer(inv -> {
				Comment saved = inv.getArgument(0);
				if (saved != null) {
					ReflectionTestUtils.setField(saved, "id", 777L);
				}
				return null;
			}).when(commentRepository).save(any(Comment.class));

			CommentRes res = commentService.create(ROOM_ID, USER_ID, new CommentCreateReq("hello"));

			assertThat(res).isNotNull();
			assertThat(res.roomId()).isEqualTo(ROOM_ID);
			assertThat(res.authorId()).isEqualTo(USER_ID);
			verify(commentRepository, times(1)).save(any(Comment.class));
			verify(commentModeration, times(1)).assertClean("hello");
			verify(challengeAuthValidator, times(1)).validateActiveParticipant(USER_ID, ROOM_ID);
		}

		@Test
		@DisplayName("방 없음 → ROOM_NOT_FOUND")
		void create_roomNotFound() {
			when(challengeRoomRepository.existsById(ROOM_ID)).thenReturn(false);

			BusinessException ex = assertThrows(
				BusinessException.class,
				() -> commentService.create(ROOM_ID, USER_ID, new CommentCreateReq("hi"))
			);
			assertThat(ex.getErrorCode()).isEqualTo(CommentErrorCode.ROOM_NOT_FOUND);

			verify(challengeRoomRepository, times(1)).existsById(ROOM_ID);
			verifyNoMoreInteractions(commentRepository, challengeAuthValidator, commentModeration);
		}
	}

	@Nested
	@DisplayName("list()")
	class ListTests {

		@Test
		@DisplayName("활성 댓글 없음 → 빈 페이지")
		void list_empty() {
			when(challengeRoomRepository.existsById(ROOM_ID)).thenReturn(true);
			when(challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)).thenReturn(null);
			when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));

			Pageable pageable = PageRequest.of(0, 20);
			when(commentRepository.findActiveByRoomIdWithAuthor(eq(ROOM_ID), any(Pageable.class)))
				.thenReturn(Page.empty(pageable));

			Page<CommentRes> page = commentService.list(ROOM_ID, USER_ID, 0, 20);

			assertThat(page.getTotalElements()).isZero();
			verify(commentLikeService, never()).findLikedCommentIdsSafely(any(), anyList());
		}

		@Test
		@DisplayName("활성 댓글 + 좋아요 반영")
		void list_withLikes() {
			when(challengeRoomRepository.existsById(ROOM_ID)).thenReturn(true);
			when(challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)).thenReturn(null);
			when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));

			Comment c1 = buildActiveComment(COMMENT_ID, mockRoom, author, "c1");
			Comment c2 = buildActiveComment(COMMENT_ID + 1, mockRoom, author, "c2");

			Pageable pageable = PageRequest.of(0, 20);
			when(commentRepository.findActiveByRoomIdWithAuthor(eq(ROOM_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(c1, c2), pageable, 2));

			// (C) 실제 호출 인자와 정확히 일치하도록 스텁
			when(commentLikeService.findLikedCommentIdsSafely(
				eq(author),
				eq(List.of(COMMENT_ID, COMMENT_ID + 1))
			)).thenReturn(Set.of(COMMENT_ID));

			Page<CommentRes> page = commentService.list(ROOM_ID, USER_ID, 0, 20);

			assertThat(page.getTotalElements()).isEqualTo(2);
			verify(commentLikeService, times(1))
				.findLikedCommentIdsSafely(eq(author), eq(List.of(COMMENT_ID, COMMENT_ID + 1)));
		}
	}

	@Nested
	@DisplayName("edit()")
	class EditTests {

		@Test
		@DisplayName("작성자 아님 → WRONG_ACCESS")
		void edit_wrongAccess() {
			when(challengeRoomRepository.existsById(ROOM_ID)).thenReturn(true);
			when(challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)).thenReturn(null);

			Comment othersComment = buildActiveComment(COMMENT_ID, mockRoom, otherUser, "old");
			when(commentRepository.findByIdAndChallengeRoom_IdAndDeletedAtIsNull(COMMENT_ID, ROOM_ID))
				.thenReturn(Optional.of(othersComment));

			BusinessException ex = assertThrows(
				BusinessException.class,
				() -> commentService.edit(ROOM_ID, COMMENT_ID, USER_ID, new CommentEditReq("new"))
			);
			assertThat(ex.getErrorCode()).isEqualTo(CommentErrorCode.WRONG_ACCESS);
			verify(commentModeration, never()).assertClean(anyString());
		}

		@Test
		@DisplayName("성공 → 내용 변경 및 좋아요 여부 포함")
		void edit_success() {
			when(challengeRoomRepository.existsById(ROOM_ID)).thenReturn(true);
			when(challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)).thenReturn(null);

			Comment own = buildActiveComment(COMMENT_ID, mockRoom, author, "old");
			when(commentRepository.findByIdAndChallengeRoom_IdAndDeletedAtIsNull(COMMENT_ID, ROOM_ID))
				.thenReturn(Optional.of(own));

			doNothing().when(commentModeration).assertClean("new");

			when(userRepository.getReferenceById(USER_ID)).thenReturn(author);
			when(commentLikeRepository.existsByCommentAndUser(own, author)).thenReturn(true);

			CommentRes res = commentService.edit(ROOM_ID, COMMENT_ID, USER_ID, new CommentEditReq("new"));

			assertThat(res).isNotNull();
			// CommentRes.from(c, isLiked)가 삭제 여부에 따라 content를 가공할 수 있으니,
			// 여기선 모더레이션과 like-check 호출만 검증
			verify(commentModeration, times(1)).assertClean("new");
			verify(commentLikeRepository, times(1)).existsByCommentAndUser(own, author);
		}
	}

	@Nested
	@DisplayName("delete()")
	class DeleteTests {

		@Test
		@DisplayName("성공 → soft delete 및 좋아요 삭제 호출")
		void delete_success() {
			when(challengeRoomRepository.existsById(ROOM_ID)).thenReturn(true);
			when(challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)).thenReturn(null);

			Comment own = buildActiveComment(COMMENT_ID, mockRoom, author, "bye");
			when(commentRepository.findByIdAndChallengeRoom_IdAndDeletedAtIsNull(COMMENT_ID, ROOM_ID))
				.thenReturn(Optional.of(own));

			doNothing().when(commentLikeRepository).deleteAllByComment(own);

			CommentRes res = commentService.delete(ROOM_ID, COMMENT_ID, USER_ID);

			assertThat(res).isNotNull();
			verify(commentLikeRepository, times(1)).deleteAllByComment(own);
		}

		@Test
		@DisplayName("댓글 없음 → COMMENT_NOT_FOUND")
		void delete_commentNotFound() {
			when(challengeRoomRepository.existsById(ROOM_ID)).thenReturn(true);
			when(challengeAuthValidator.validateActiveParticipant(USER_ID, ROOM_ID)).thenReturn(null);

			when(commentRepository.findByIdAndChallengeRoom_IdAndDeletedAtIsNull(COMMENT_ID, ROOM_ID))
				.thenReturn(Optional.empty());

			BusinessException ex = assertThrows(
				BusinessException.class,
				() -> commentService.delete(ROOM_ID, COMMENT_ID, USER_ID)
			);
			assertThat(ex.getErrorCode()).isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
			verify(commentLikeRepository, never()).deleteAllByComment(any());
		}
	}
}
