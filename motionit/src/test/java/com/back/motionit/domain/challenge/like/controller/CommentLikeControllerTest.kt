package com.back.motionit.domain.challenge.like.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.back.motionit.domain.challenge.comment.entity.Comment;
import com.back.motionit.domain.challenge.comment.repository.CommentRepository;
import com.back.motionit.domain.challenge.like.entity.CommentLike;
import com.back.motionit.domain.challenge.like.repository.CommentLikeRepository;
import com.back.motionit.domain.challenge.like.service.CommentLikeService;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.global.error.code.CommentLikeErrorCode;
import com.back.motionit.helper.ChallengeParticipantHelper;
import com.back.motionit.helper.ChallengeRoomHelper;
import com.back.motionit.helper.CommentHelper;
import com.back.motionit.helper.UserHelper;
import com.back.motionit.security.SecurityUser;
import com.back.motionit.support.BaseIntegrationTest;
import com.back.motionit.support.SecuredIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

@SecuredIntegrationTest
public class CommentLikeControllerTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private CommentLikeRepository commentLikeRepository;

	@Autowired
	private UserHelper userHelper;

	@Autowired
	private ChallengeRoomHelper roomHelper;

	@Autowired
	private CommentHelper commentHelper;

	@Autowired
	private ChallengeParticipantHelper participantHelper;

	@Autowired
	private CommentLikeService commentLikeService;

	private ObjectMapper mapper = new ObjectMapper();
	private User user;
	private ChallengeRoom room;
	private Comment comment;
	private SecurityUser securityUser;
	private UsernamePasswordAuthenticationToken authentication;

	@BeforeEach
	public void setUp() {
		user = userHelper.createUser();
		room = roomHelper.createChallengeRoom(user);
		comment = commentHelper.createComment(user, room, "테스트 댓글입니다.");

		var authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
		securityUser = new SecurityUser(user.getId(), user.getPassword(), user.getNickname(), authorities);
		authentication = new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	@Nested
	@DisplayName("POST `/api/v1/comments/{commentId}/likes` - 댓글 좋아요 토글")
	class ToggleCommentLikeTest {
		private String toggleLikeApi = "/api/v1/comments/{commentId}/likes";

		@Test
		@DisplayName("좋아요 생성 성공")
		void successCreateLike() throws Exception {
			// when
			ResultActions resultActions = mvc.perform(
				post(toggleLikeApi, comment.getId())
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then
			resultActions
				.andExpect(handler().handlerType(CommentLikeController.class))
				.andExpect(handler().methodName("toggleCommentLikeByCommentId"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultCode").value("200"))
				.andExpect(jsonPath("$.msg").value("좋아요 성공"))
				.andExpect(jsonPath("$.data.id").value(comment.getId()))
				.andExpect(jsonPath("$.data.isLiked").value(true))
				.andExpect(jsonPath("$.data.likeCount").value(1));

			// 실제 DB 확인
			Comment updatedComment = commentRepository.findById(comment.getId()).orElseThrow();
			assertThat(updatedComment.getLikeCount()).isEqualTo(1);
			assertThat(commentLikeRepository.existsByCommentAndUser(updatedComment, user)).isTrue();
		}

		@Test
		@DisplayName("좋아요 취소 성공")
		void successCancelLike() throws Exception {
			// given - 먼저 좋아요 생성
			CommentLike like = CommentLike.builder()
				.comment(comment)
				.user(user)
				.build();
			commentLikeRepository.save(like);
			comment.incrementLikeCount();
			commentRepository.save(comment);

			// when
			ResultActions resultActions = mvc.perform(
				post(toggleLikeApi, comment.getId())
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then
			resultActions
				.andExpect(handler().handlerType(CommentLikeController.class))
				.andExpect(handler().methodName("toggleCommentLikeByCommentId"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultCode").value("200"))
				.andExpect(jsonPath("$.msg").value("좋아요 취소 성공"))
				.andExpect(jsonPath("$.data.id").value(comment.getId()))
				.andExpect(jsonPath("$.data.isLiked").value(false))
				.andExpect(jsonPath("$.data.likeCount").value(0));

			// 실제 DB 확인
			Comment updatedComment = commentRepository.findById(comment.getId()).orElseThrow();
			assertThat(updatedComment.getLikeCount()).isEqualTo(0);
			assertThat(commentLikeRepository.existsByCommentAndUser(updatedComment, user)).isFalse();
		}

		@Test
		@DisplayName("좋아요 토글 여러 번 수행")
		void toggleLikeMultipleTimes() throws Exception {
			// 1. 좋아요 생성
			mvc.perform(post(toggleLikeApi, comment.getId())
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.data.isLiked").value(true))
				.andExpect(jsonPath("$.data.likeCount").value(1));

			// 2. 좋아요 취소
			mvc.perform(post(toggleLikeApi, comment.getId())
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.data.isLiked").value(false))
				.andExpect(jsonPath("$.data.likeCount").value(0));

			// 3. 다시 좋아요 생성
			mvc.perform(post(toggleLikeApi, comment.getId())
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.data.isLiked").value(true))
				.andExpect(jsonPath("$.data.likeCount").value(1));

			// 최종 상태 확인
			Comment finalComment = commentRepository.findById(comment.getId()).orElseThrow();
			assertThat(finalComment.getLikeCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("존재하지 않는 댓글에 좋아요 시도 - 실패")
		void failedLikeNonExistentComment() throws Exception {
			// given
			Long nonExistentCommentId = comment.getId() + 999L;

			// when
			ResultActions resultActions = mvc.perform(
				post(toggleLikeApi, nonExistentCommentId)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			// then
			CommentLikeErrorCode error = CommentLikeErrorCode.COMMENT_NOT_FOUND;
			resultActions
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.resultCode").value(error.getCode()))
				.andExpect(jsonPath("$.msg").value(error.getMessage()));
		}

		@Test
		@DisplayName("여러 사용자가 동일 댓글에 좋아요")
		void multipleLikesFromDifferentUsers() throws Exception {
			// given - 3명의 사용자 생성
			List<User> users = new ArrayList<>();
			for (int i = 0; i < 3; i++) {
				users.add(userHelper.createUser());
			}

			// when - 각 사용자가 좋아요
			for (User u : users) {
				var authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
				var secUser = new SecurityUser(u.getId(), u.getPassword(), u.getNickname(), authorities);
				var auth = new UsernamePasswordAuthenticationToken(secUser, null, secUser.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(auth);

				mvc.perform(post(toggleLikeApi, comment.getId())
						.contentType(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
			}

			// then
			Comment likedComment = commentRepository.findById(comment.getId()).orElseThrow();
			assertThat(likedComment.getLikeCount()).isEqualTo(3);
			assertThat(commentLikeRepository.count()).isEqualTo(3);
		}

		@Test
		@DisplayName("좋아요 수가 0 미만으로 감소하지 않음")
		void likeCountNotBelowZero() throws Exception {
			// given - 좋아요가 없는 상태에서 취소 시도
			assertThat(comment.getLikeCount()).isEqualTo(0);

			// when - 좋아요 토글 (생성 후 즉시 취소)
			mvc.perform(post(toggleLikeApi, comment.getId())
				.contentType(MediaType.APPLICATION_JSON));

			mvc.perform(post(toggleLikeApi, comment.getId())
				.contentType(MediaType.APPLICATION_JSON));

			// then
			Comment finalComment = commentRepository.findById(comment.getId()).orElseThrow();
			assertThat(finalComment.getLikeCount()).isEqualTo(0);
		}
	}

	@Nested
	@DisplayName("좋아요 중복 방지 및 동시성 테스트")
	class ConcurrencyTest {

		@Test
		@DisplayName("동일 사용자의 중복 좋아요 방지 - DB 제약조건")
		void preventDuplicateLikesBySameUser() {
			// given
			CommentLike like1 = CommentLike.builder()
				.comment(comment)
				.user(user)
				.build();
			commentLikeRepository.save(like1);

			// when & then - 동일 사용자의 중복 좋아요 시도
			CommentLike like2 = CommentLike.builder()
				.comment(comment)
				.user(user)
				.build();

			assertThatThrownBy(() -> {
				commentLikeRepository.save(like2);
				commentLikeRepository.flush(); // 즉시 DB 반영하여 제약조건 검증
			}).isInstanceOf(Exception.class);
		}

		@Test
		@DisplayName("동시 좋아요 요청 처리 - 낙관적 락 적용")
		void handleConcurrentLikeRequests() throws InterruptedException {
			// given
			int threadCount = 10;
			ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
			CountDownLatch latch = new CountDownLatch(threadCount);
			AtomicInteger successCount = new AtomicInteger(0);
			AtomicInteger failCount = new AtomicInteger(0);

			// when - 동일 사용자가 동시에 좋아요 토글
			for (int i = 0; i < threadCount; i++) {
				executorService.submit(() -> {
					try {
						commentLikeService.toggleCommentLikeByCommentId(comment.getId(), user.getId());
						successCount.incrementAndGet();
					} catch (Exception e) {
						failCount.incrementAndGet();
					} finally {
						latch.countDown();
					}
				});
			}

			latch.await();
			executorService.shutdown();

			// then - 최종 상태 확인
			Comment finalComment = commentRepository.findById(comment.getId()).orElseThrow();

			// 성공한 요청 수만큼 좋아요 상태가 토글됨
			// 짝수면 좋아요 취소 상태, 홀수면 좋아요 상태
			boolean expectedLikeState = (successCount.get() % 2) == 1;
			int expectedLikeCount = expectedLikeState ? 1 : 0;

			assertThat(finalComment.getLikeCount()).isEqualTo(expectedLikeCount);
			assertThat(commentLikeRepository.existsByCommentAndUser(finalComment, user))
				.isEqualTo(expectedLikeState);
		}
	}

	@Nested
	@DisplayName("댓글 조회 시 좋아요 정보 표시")
	class CommentListWithLikeInfo {
		private String getCommentsApi = "/api/v1/rooms/{roomId}/comments";

		@Test
		@DisplayName("댓글 목록 조회 시 좋아요 정보 포함")
		void getCommentsWithLikeInfo() throws Exception {
			// given - 새로운 룸과 댓글 생성
			User testUser = userHelper.createUser();
			ChallengeRoom testRoom = roomHelper.createChallengeRoom(testUser);

			// 챌린지 참가자 생성 (필수)
			participantHelper.createNormalParticipant(testUser, testRoom);

			Comment comment1 = commentHelper.createComment(testUser, testRoom, "첫 번째 댓글");
			Comment comment2 = commentHelper.createComment(testUser, testRoom, "두 번째 댓글");
			Comment comment3 = commentHelper.createComment(testUser, testRoom, "세 번째 댓글");

			// comment1에만 현재 사용자가 좋아요
			CommentLike like = CommentLike.builder()
				.comment(comment1)
				.user(testUser)
				.build();
			commentLikeRepository.save(like);
			comment1.incrementLikeCount();
			commentRepository.save(comment1);

			// comment2에는 다른 사용자가 좋아요
			User otherUser = userHelper.createUser();
			CommentLike otherLike = CommentLike.builder()
				.comment(comment2)
				.user(otherUser)
				.build();
			commentLikeRepository.save(otherLike);
			comment2.incrementLikeCount();
			commentRepository.save(comment2);

			// 인증 설정
			var authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
			var testSecurityUser = new SecurityUser(testUser.getId(), testUser.getPassword(), testUser.getNickname(),
				authorities);
			var testAuth = new UsernamePasswordAuthenticationToken(testSecurityUser, null,
				testSecurityUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(testAuth);

			// when
			MvcResult result = mvc.perform(
					get(getCommentsApi, testRoom.getId())
						.contentType(MediaType.APPLICATION_JSON)
				).andDo(print())
				.andExpect(status().isOk())
				.andReturn();

			// then
			String responseJson = result.getResponse().getContentAsString();

			// comment1 검증 (현재 사용자가 좋아요한 댓글)
			List<Integer> comment1LikeCounts = JsonPath.read(responseJson,
				"$.data.content[?(@.id == " + comment1.getId() + ")].likeCount");
			List<Boolean> comment1IsLiked = JsonPath.read(responseJson,
				"$.data.content[?(@.id == " + comment1.getId() + ")].isLiked");
			assertThat(comment1LikeCounts).hasSize(1);
			assertThat(comment1LikeCounts.get(0)).isEqualTo(1);
			assertThat(comment1IsLiked.get(0)).isTrue();

			// comment2 검증 (다른 사용자가 좋아요한 댓글)
			List<Integer> comment2LikeCounts = JsonPath.read(responseJson,
				"$.data.content[?(@.id == " + comment2.getId() + ")].likeCount");
			List<Boolean> comment2IsLiked = JsonPath.read(responseJson,
				"$.data.content[?(@.id == " + comment2.getId() + ")].isLiked");
			assertThat(comment2LikeCounts).hasSize(1);
			assertThat(comment2LikeCounts.get(0)).isEqualTo(1);
			assertThat(comment2IsLiked.get(0)).isFalse();

			// comment3 검증 (좋아요 없는 댓글)
			List<Integer> comment3LikeCounts = JsonPath.read(responseJson,
				"$.data.content[?(@.id == " + comment3.getId() + ")].likeCount");
			List<Boolean> comment3IsLiked = JsonPath.read(responseJson,
				"$.data.content[?(@.id == " + comment3.getId() + ")].isLiked");
			assertThat(comment3LikeCounts).hasSize(1);
			assertThat(comment3LikeCounts.get(0)).isEqualTo(0);
			assertThat(comment3IsLiked.get(0)).isFalse();
		}
	}

	@Nested
	@DisplayName("댓글 삭제 시 좋아요 처리")
	class DeleteCommentWithLikes {
		private String deleteCommentApi = "/api/v1/rooms/{roomId}/comments/{commentId}";

		@Test
		@DisplayName("댓글 삭제 시 관련 좋아요도 모두 삭제")
		void deleteLikesWhenCommentDeleted() throws Exception {
			// given - 새로운 사용자와 댓글 생성
			User testUser = userHelper.createUser();
			ChallengeRoom testRoom = roomHelper.createChallengeRoom(testUser);

			// 챌린지 참가자 생성 (필수)
			participantHelper.createNormalParticipant(testUser, testRoom);

			Comment testComment = commentHelper.createComment(testUser, testRoom, "삭제될 댓글");

			// 여러 사용자가 좋아요
			User user1 = userHelper.createUser();
			User user2 = userHelper.createUser();

			CommentLike like1 = CommentLike.builder().comment(testComment).user(testUser).build();
			CommentLike like2 = CommentLike.builder().comment(testComment).user(user1).build();
			CommentLike like3 = CommentLike.builder().comment(testComment).user(user2).build();

			commentLikeRepository.saveAll(List.of(like1, like2, like3));
			testComment.incrementLikeCount();
			testComment.incrementLikeCount();
			testComment.incrementLikeCount();
			commentRepository.save(testComment);

			// 삭제 전 이 댓글의 좋아요 수 확인
			long likesForThisComment = commentLikeRepository.findAll().stream()
				.filter(like -> like.getComment().getId().equals(testComment.getId()))
				.count();
			assertThat(likesForThisComment).isEqualTo(3);

			// 인증 설정
			var authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
			var testSecurityUser = new SecurityUser(testUser.getId(), testUser.getPassword(), testUser.getNickname(),
				authorities);
			var testAuth = new UsernamePasswordAuthenticationToken(testSecurityUser, null,
				testSecurityUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(testAuth);

			// when - 댓글 삭제
			mvc.perform(
					delete(deleteCommentApi, testRoom.getId(), testComment.getId())
						.contentType(MediaType.APPLICATION_JSON)
				).andDo(print())
				.andExpect(status().isOk());

			// then - 해당 댓글의 좋아요가 모두 삭제됨
			long likesAfterDelete = commentLikeRepository.findAll().stream()
				.filter(like -> like.getComment().getId().equals(testComment.getId()))
				.count();
			assertThat(likesAfterDelete).isEqualTo(0);

			// 댓글이 soft delete 되었는지 확인
			Comment deletedComment = commentRepository.findById(testComment.getId()).orElseThrow();
			assertThat(deletedComment.isDeleted()).isTrue();
		}
	}

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}
}
