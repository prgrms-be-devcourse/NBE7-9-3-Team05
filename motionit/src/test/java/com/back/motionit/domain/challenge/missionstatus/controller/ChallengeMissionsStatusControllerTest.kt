package com.back.motionit.domain.challenge.missionstatus.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

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

import com.back.motionit.domain.challenge.mission.api.response.ChallengeMissionStatusHttp;
import com.back.motionit.domain.challenge.mission.dto.ChallengeMissionCompleteRequest;
import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus;
import com.back.motionit.domain.challenge.mission.repository.ChallengeMissionStatusRepository;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo;
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.factory.ChallengeMissionStatusFactory;
import com.back.motionit.factory.ChallengeParticipantFactory;
import com.back.motionit.factory.ChallengeRoomFactory;
import com.back.motionit.factory.ChallengeVideoFactory;
import com.back.motionit.global.error.code.ChallengeMissionErrorCode;
import com.back.motionit.helper.UserHelper;
import com.back.motionit.security.SecurityUser;
import com.back.motionit.support.BaseIntegrationTest;
import com.back.motionit.support.SecuredIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

@SecuredIntegrationTest
public class ChallengeMissionsStatusControllerTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ChallengeMissionStatusRepository challengeMissionStatusRepository;

	@Autowired
	private ChallengeParticipantRepository challengeParticipantRepository;

	@Autowired
	private ChallengeRoomRepository challengeRoomRepository;

	@Autowired
	private ChallengeVideoRepository challengeVideoRepository;

	@Autowired
	private UserHelper userHelper;

	private ChallengeRoom room;
	private ChallengeParticipant participant;
	private ChallengeVideo video;
	private LocalDate today;
	private User user;

	SecurityUser securityUser;
	UsernamePasswordAuthenticationToken authentication;

	@BeforeEach
	void setUp() {
		// 기본 사용자 및 데이터 구성
		user = userHelper.createUser();
		room = challengeRoomRepository.save(ChallengeRoomFactory.fakeChallengeRoom(user, 5));
		participant = challengeParticipantRepository.save(
			ChallengeParticipantFactory.fakeParticipant(user, room)
		);
		video = challengeVideoRepository.save(ChallengeVideoFactory.fakeChallengeVideo(user, room));
		today = LocalDate.now();

		challengeMissionStatusRepository.save(ChallengeMissionStatusFactory.fakeMission(participant));

		// ChallengeRoomControllerTest와 동일한 인증 세팅
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
	@DisplayName("POST /rooms/{roomId}/missions/complete - 미션 완료 성공")
	void completeMissionSuccess() throws Exception {
		ChallengeMissionCompleteRequest request = new ChallengeMissionCompleteRequest(video.getId());

		mvc.perform(
				post("/api/v1/challenge/rooms/{roomId}/missions/complete", room.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.msg").value(ChallengeMissionStatusHttp.MISSION_COMPLETE_SUCCESS_MESSAGE))
			.andExpect(jsonPath("$.data.completed").value(true))
			.andDo(print());
	}

	@Test
	@DisplayName("미션 완료 실패 - 이미 완료된 미션")
	void completeMission_alreadyCompleted() throws Exception {
		ChallengeMissionStatus mission = challengeMissionStatusRepository
			.findByParticipantIdAndMissionDate(participant.getId(), today)
			.orElseThrow();
		mission.completeMission();
		challengeMissionStatusRepository.save(mission);

		ChallengeMissionCompleteRequest request = new ChallengeMissionCompleteRequest(video.getId());

		mvc.perform(
				post("/api/v1/challenge/rooms/{roomId}/missions/complete", room.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
			)
			.andExpect(status().is4xxClientError())
			.andExpect(jsonPath("$.msg").value(ChallengeMissionErrorCode.ALREADY_COMPLETED.getMessage()))
			.andDo(print());
	}

	@Test
	@DisplayName("GET /rooms/{roomId}/missions/personal/today - 개인 오늘 미션 조회 성공")
	void getTodayMissionStatus_success() throws Exception {
		mvc.perform(
				get("/api/v1/challenge/rooms/{roomId}/missions/personal/today", room.getId())
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.msg").value(ChallengeMissionStatusHttp.GET_TODAY_PARTICIPANT_SUCCESS_MESSAGE))
			.andExpect(jsonPath("$.data.participantId").value(participant.getId()))
			.andDo(print());
	}

	@Test
	@DisplayName("GET /rooms/{roomId}/missions/today - 방 전체 오늘의 미션 조회 성공")
	void getTodayMissionByRoom_success() throws Exception {
		mvc.perform(
				get("/api/v1/challenge/rooms/{roomId}/missions/today", room.getId())
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.msg").value(ChallengeMissionStatusHttp.GET_TODAY_SUCCESS_MESSAGE))
			.andExpect(jsonPath("$.data").isArray())
			.andDo(print());
	}

	@Test
	@DisplayName("GET /rooms/{roomId}/missions/personal/history - 개인 미션 히스토리 조회 성공")
	void getMissionHistory_success() throws Exception {
		mvc.perform(
				get("/api/v1/challenge/rooms/{roomId}/missions/personal/history", room.getId())
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.msg").value(ChallengeMissionStatusHttp.GET_MISSION_HISTORY_SUCCESS_MESSAGE))
			.andExpect(jsonPath("$.data").isArray())
			.andDo(print());
	}
}
