package com.back.motionit.domain.challenge.video.controller;

import static org.assertj.core.api.Assertions.*;
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
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.challenge.video.api.response.ChallengeVideoHttp;
import com.back.motionit.domain.challenge.video.dto.ChallengeVideoUploadRequest;
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo;
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.factory.ChallengeParticipantFactory;
import com.back.motionit.factory.ChallengeRoomFactory;
import com.back.motionit.factory.ChallengeVideoFactory;
import com.back.motionit.global.error.code.ChallengeParticipantErrorCode;
import com.back.motionit.helper.UserHelper;
import com.back.motionit.security.SecurityUser;
import com.back.motionit.support.BaseIntegrationTest;
import com.back.motionit.support.SecuredIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

@SecuredIntegrationTest
class ChallengeVideoControllerTest extends BaseIntegrationTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	ChallengeVideoRepository challengeVideoRepository;
	@Autowired
	ChallengeRoomRepository challengeRoomRepository;
	@Autowired
	ChallengeParticipantRepository challengeParticipantRepository;
	@Autowired
	UserHelper userHelper;
	@Autowired
	private RequestMappingHandlerAdapter adapter;

	private User user;
	private ChallengeRoom room;

	SecurityUser securityUser;
	UsernamePasswordAuthenticationToken authentication;

	@BeforeEach
	void setUp() {
		challengeVideoRepository.deleteAll();

		// 기본 사용자 및 방, 참가자 세팅
		user = userHelper.createUser();
		room = challengeRoomRepository.save(ChallengeRoomFactory.fakeChallengeRoom(user, 5));
		challengeParticipantRepository.save(
			ChallengeParticipantFactory.fakeParticipant(user, room)
		);

		// 인증 세팅 (ChallengeRoomControllerTest와 동일)
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

	@Test
	public void printConverters() {
		adapter.getMessageConverters().forEach(c ->
			System.out.println("✅ " + c.getClass().getName())
		);
	}

	@Test
	@DisplayName("POST `/api/v1/challenge/rooms/{roomId}/videos` - 영상 업로드 성공")
	void successUploadVideo() throws Exception {
		// given
		ChallengeVideoUploadRequest request = new ChallengeVideoUploadRequest(
			"https://www.youtube.com/watch?v=dQw4w9WgXcQ"
		);
		String requestJson = objectMapper.writeValueAsString(request);

		// when & then
		mvc.perform(post("/api/v1/challenge/rooms/{roomId}/videos", room.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.characterEncoding("UTF-8")
				.content(requestJson))
			.andExpect(status().isOk())
			.andExpect(handler().handlerType(ChallengeVideoController.class))
			.andExpect(handler().methodName("uploadVideo"))
			.andExpect(jsonPath("$.msg").value(ChallengeVideoHttp.UPLOAD_SUCCESS_MESSAGE))
			.andDo(print());
	}

	@Test
	@DisplayName("POST `/api/v1/challenge/rooms/{roomId}/videos` - 참가자가 아닌 경우 업로드 실패")
	void failUploadVideo_NotParticipant() throws Exception {
		// given - 다른 방 생성, 인증유저는 참여 안함
		ChallengeRoom otherRoom = challengeRoomRepository.save(
			ChallengeRoomFactory.fakeChallengeRoom(userHelper.createUser(), 5)
		);

		ChallengeVideoUploadRequest request = new ChallengeVideoUploadRequest(
			"https://www.youtube.com/watch?v=dQw4w9WgXcQ"
		);
		String requestJson = objectMapper.writeValueAsString(request);

		// when & then
		mvc.perform(post("/api/v1/challenge/rooms/{roomId}/videos", otherRoom.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson))
			.andExpect(status().is4xxClientError())
			.andExpect(jsonPath("$.msg").value(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM.getMessage()))
			.andDo(print());
	}

	@Test
	@DisplayName("GET `/api/v1/challenge/rooms/{roomId}/videos/today` - 오늘의 미션 영상 조회 성공")
	void successGetTodayMissionVideos() throws Exception {
		// given
		challengeVideoRepository.save(ChallengeVideoFactory.fakeChallengeVideo(user, room));
		challengeVideoRepository.save(ChallengeVideoFactory.fakeChallengeVideo(user, room));

		// when & then
		mvc.perform(get("/api/v1/challenge/rooms/{roomId}/videos/today", room.getId())
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(handler().handlerType(ChallengeVideoController.class))
			.andExpect(handler().methodName("getTodayMissionVideos"))
			.andExpect(jsonPath("$.msg").value(ChallengeVideoHttp.GET_TODAY_MISSION_SUCCESS_MESSAGE))
			.andExpect(jsonPath("$.data").isArray())
			.andDo(print());
	}

	@Test
	@DisplayName("GET `/api/v1/challenge/rooms/{roomId}/videos/today` - 참가자가 아닌 경우 실패")
	void failGetTodayMissionVideos_NotParticipant() throws Exception {
		ChallengeRoom otherRoom = challengeRoomRepository.save(
			ChallengeRoomFactory.fakeChallengeRoom(userHelper.createUser(), 5)
		);

		mvc.perform(get("/api/v1/challenge/rooms/{roomId}/videos/today", otherRoom.getId())
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().is4xxClientError())
			.andExpect(jsonPath("$.msg").value(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM.getMessage()))
			.andDo(print());
	}

	@Test
	@DisplayName("DELETE `/api/v1/challenge/rooms/{roomId}/videos/{videoId}` - 영상 삭제 성공")
	void successDeleteVideo() throws Exception {
		// given
		ChallengeVideo video = challengeVideoRepository.save(
			ChallengeVideoFactory.fakeChallengeVideo(user, room)
		);

		// when & then
		mvc.perform(delete("/api/v1/challenge/rooms/{roomId}/videos/{videoId}", room.getId(), video.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(handler().handlerType(ChallengeVideoController.class))
			.andExpect(handler().methodName("deleteVideoByUser"))
			.andExpect(jsonPath("$.msg").value(ChallengeVideoHttp.DELETE_SUCCESS_MESSAGE))
			.andDo(print());

		assertThat(challengeVideoRepository.findById(video.getId())).isEmpty();
	}

	@Test
	@DisplayName("DELETE `/api/v1/challenge/rooms/{roomId}/videos/{videoId}` - 참가자가 아닐 경우 삭제 실패")
	void failDeleteVideo_NotParticipant() throws Exception {
		// given
		User otherUser = userHelper.createUser();
		ChallengeRoom otherRoom = challengeRoomRepository.save(ChallengeRoomFactory.fakeChallengeRoom(otherUser, 5));
		ChallengeVideo video = challengeVideoRepository.save(
			ChallengeVideoFactory.fakeChallengeVideo(otherUser, otherRoom));

		// when & then
		mvc.perform(delete("/api/v1/challenge/rooms/{roomId}/videos/{videoId}", otherRoom.getId(), video.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().is4xxClientError())
			.andExpect(jsonPath("$.msg").value(ChallengeParticipantErrorCode.NO_PARTICIPANT_IN_ROOM.getMessage()))
			.andDo(print());
	}
}
