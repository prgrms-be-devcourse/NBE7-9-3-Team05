package com.back.motionit.domain.challenge.participant.controller;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.back.motionit.domain.challenge.participant.api.response.ChallengeParticipantHttp;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole;
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.factory.ChallengeRoomFactory;
import com.back.motionit.global.error.code.ChallengeParticipantErrorCode;
import com.back.motionit.helper.UserHelper;
import com.back.motionit.security.SecurityUser;
import com.back.motionit.support.BaseIntegrationTest;
import com.back.motionit.support.SecuredIntegrationTest;

@SecuredIntegrationTest
public class ChallengeParticipantControllerTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ChallengeParticipantRepository challengeParticipantRepository;

	@Autowired
	private ChallengeRoomRepository challengeRoomRepository;

	@Autowired
	private UserHelper userHelper;

	private User user;
	private ChallengeRoom room;

	SecurityUser securityUser;
	UsernamePasswordAuthenticationToken authentication;

	@BeforeEach
	void setUp() {
		user = userHelper.createUser();
		room = createTestRoom(user);
		var authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
		securityUser = new SecurityUser(user.getId(), user.getPassword(), user.getNickname(), authorities);
		authentication =
			new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private ChallengeRoom createTestRoom(User owner) {
		ChallengeRoom room = ChallengeRoomFactory.fakeChallengeRoom(owner);
		return challengeRoomRepository.save(room);
	}

	@Test
	@DisplayName("POST `/api/v1/challenge/participants/{roomId}/join` - 성공적으로 방 참가")
	void successJoinChallengeRoom() throws Exception {
		mvc.perform(
				post("/api/v1/challenge/participants/{roomId}/join", room.getId())
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andDo(print())
			.andExpect(handler().handlerType(ChallengeParticipantController.class))
			.andExpect(handler().methodName("joinChallengeRoom"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.msg").value(ChallengeParticipantHttp.JOIN_SUCCESS_MESSAGE));

		ChallengeParticipant participant =
			challengeParticipantRepository.findByUserAndChallengeRoom(user, room);

		assertThat(participant).isNotNull();
		assertThat(participant.getQuited()).isFalse();
	}

	@Test
	@DisplayName("POST `/api/v1/challenge/participants/{roomId}/join` - 이미 참가중일 경우 실패")
	void failWhenAlreadyJoined() throws Exception {
		// given - 1회 참가
		mvc.perform(
			post("/api/v1/challenge/participants/{roomId}/join", room.getId())
				.contentType(MediaType.APPLICATION_JSON)
		).andDo(print());

		// when - 중복 참가
		ResultActions resultActions = mvc.perform(
			post("/api/v1/challenge/participants/{roomId}/join", room.getId())
				.contentType(MediaType.APPLICATION_JSON)
		).andDo(print());

		ChallengeParticipantErrorCode error = ChallengeParticipantErrorCode.ALREADY_JOINED;

		resultActions
			.andExpect(handler().handlerType(ChallengeParticipantController.class))
			.andExpect(handler().methodName("joinChallengeRoom"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.resultCode").value(error.getCode()))
			.andExpect(jsonPath("$.msg").value(error.getMessage()));
	}

	@Test
	@DisplayName("POST `/api/v1/challenge/participants/{roomId}/join` - 존재하지 않는 방일 경우 실패")
	void failWhenRoomNotFound() throws Exception {
		Long wrongRoomId = room.getId() + 1L;

		ResultActions resultActions = mvc.perform(
			post("/api/v1/challenge/participants/{roomId}/join", wrongRoomId)
				.contentType(MediaType.APPLICATION_JSON)
		).andDo(print());

		ChallengeParticipantErrorCode error = ChallengeParticipantErrorCode.CANNOT_FIND_CHALLENGE_ROOM;

		resultActions
			.andExpect(handler().handlerType(ChallengeParticipantController.class))
			.andExpect(handler().methodName("joinChallengeRoom"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.resultCode").value(error.getCode()))
			.andExpect(jsonPath("$.msg").value(error.getMessage()));
	}

	@Test
	@DisplayName("POST `/api/v1/challenge/participants/{roomId}/join` - 정원 초과 시 실패")
	void failWhenRoomIsFull() throws Exception {
		// given - 정원 2인 방
		ChallengeRoom smallRoom = challengeRoomRepository.save(
			ChallengeRoomFactory.fakeChallengeRoom(user, 2)
		);

		User userA = userHelper.createUser();
		User userB = userHelper.createUser();
		challengeParticipantRepository.save(
			new ChallengeParticipant(userA, smallRoom, ChallengeParticipantRole.NORMAL));
		challengeParticipantRepository.save(
			new ChallengeParticipant(userB, smallRoom, ChallengeParticipantRole.NORMAL));

		ResultActions resultActions = mvc.perform(
			post("/api/v1/challenge/participants/{roomId}/join", smallRoom.getId())
				.contentType(MediaType.APPLICATION_JSON)
		).andDo(print());

		ChallengeParticipantErrorCode error = ChallengeParticipantErrorCode.FULL_JOINED_ROOM;

		resultActions
			.andExpect(handler().handlerType(ChallengeParticipantController.class))
			.andExpect(handler().methodName("joinChallengeRoom"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.resultCode").value(error.getCode()))
			.andExpect(jsonPath("$.msg").value(error.getMessage()));
	}

	@Test
	@DisplayName("POST `/api/v1/challenge/participants/{roomId}/leave` - 성공적으로 방 탈퇴")
	void successLeaveChallengeRoom() throws Exception {
		// given - 참가
		mvc.perform(
			post("/api/v1/challenge/participants/{roomId}/join", room.getId())
				.contentType(MediaType.APPLICATION_JSON)
		).andDo(print());

		// when - 탈퇴
		ResultActions resultActions = mvc.perform(
			post("/api/v1/challenge/participants/{roomId}/leave", room.getId())
				.contentType(MediaType.APPLICATION_JSON)
		).andDo(print());

		resultActions
			.andExpect(handler().handlerType(ChallengeParticipantController.class))
			.andExpect(handler().methodName("leaveChallengeRoom"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.resultCode").value(ChallengeParticipantHttp.LEAVE_SUCCESS_CODE))
			.andExpect(jsonPath("$.msg").value(ChallengeParticipantHttp.LEAVE_SUCCESS_MESSAGE));

		ChallengeParticipant updated = challengeParticipantRepository
			.findByUserAndChallengeRoom(user, room);

		assertNotNull(updated, "참가자가 조회되어야 합니다.");
		assertThat(updated.getQuited()).isTrue();
		assertThat(updated.getQuitDate()).isNotNull();
	}

	@Test
	@DisplayName("POST `/api/v1/challenge/participants/{roomId}/leave` - 방에 참가중이지 않으면 실패")
	void failWhenNotParticipant() throws Exception {
		ResultActions resultActions = mvc.perform(
			post("/api/v1/challenge/participants/{roomId}/leave", room.getId())
				.contentType(MediaType.APPLICATION_JSON)
		).andDo(print());

		ChallengeParticipantErrorCode error = ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM;

		resultActions
			.andExpect(handler().handlerType(ChallengeParticipantController.class))
			.andExpect(handler().methodName("leaveChallengeRoom"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.resultCode").value(error.getCode()))
			.andExpect(jsonPath("$.msg").value(error.getMessage()));
	}

	@Test
	@DisplayName("GET `/api/v1/challenge/participants/{roomId}/status` - 현재 방 참가 상태 조회 성공")
	void getParticipationStatus_success() throws Exception {
		// given - 참가 처리
		mvc.perform(
			post("/api/v1/challenge/participants/{roomId}/join", room.getId())
				.contentType(MediaType.APPLICATION_JSON)
		).andDo(print());

		mvc.perform(
				get("/api/v1/challenge/participants/{roomId}/status", room.getId())
					.accept(MediaType.APPLICATION_JSON)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.msg").value(ChallengeParticipantHttp.GET_PARTICIPANT_STATUS_SUCCESS_MESSAGE))
			.andExpect(jsonPath("$.data.joined").value(true));
	}
}
