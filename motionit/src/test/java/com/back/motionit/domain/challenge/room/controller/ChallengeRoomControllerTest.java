package com.back.motionit.domain.challenge.room.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Map;

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

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole;
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;
import com.back.motionit.domain.challenge.room.api.response.ChallengeRoomHttp;
import com.back.motionit.domain.challenge.room.builder.CreateRoomRequestBuilder;
import com.back.motionit.domain.challenge.room.dto.GetRoomsResponse;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.global.constants.ChallengeRoomConstants;
import com.back.motionit.global.error.code.ChallengeRoomErrorCode;
import com.back.motionit.global.error.code.CommonErrorCode;
import com.back.motionit.global.error.exception.BusinessException;
import com.back.motionit.global.service.AwsS3Service;
import com.back.motionit.helper.ChallengeParticipantHelper;
import com.back.motionit.helper.ChallengeRoomHelper;
import com.back.motionit.helper.UserHelper;
import com.back.motionit.security.SecurityUser;
import com.back.motionit.support.BaseIntegrationTest;
import com.back.motionit.support.SecuredIntegrationTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

@SecuredIntegrationTest
public class ChallengeRoomControllerTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ChallengeRoomRepository challengeRoomRepository;

	@Autowired
	private ChallengeParticipantRepository challengeParticipantRepository;

	@Autowired
	private UserHelper userHelper;

	@Autowired
	private ChallengeRoomHelper roomHelper;

	@Autowired
	private ChallengeParticipantHelper participantHelper;

	@Autowired
	private ObjectMapper mapper;

	private CreateRoomRequestBuilder createRoomRequestBuilder;
	private User user;

	SecurityUser securityUser;
	UsernamePasswordAuthenticationToken authentication;

	@BeforeEach
	public void setUp() {
		createRoomRequestBuilder = new CreateRoomRequestBuilder("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
		user = userHelper.createUser();
	}

	@Nested
	@DisplayName("POST `/api/v1/challenge/rooms` - 운동방 생성")
	class CreateRoomTest {
		private String baseRoomApi = "/api/v1/challenge/rooms";

		@Test
		@DisplayName("Success Create Challenge Room")
		void successCreateRoom() throws Exception {
			Map<String, String> params = createRoomRequestBuilder.toParamMap();

			var authorities = AuthorityUtils.createAuthorityList("ROLE");
			securityUser = new SecurityUser(user.getId(), user.getPassword(), user.getNickname(), authorities);
			authentication =
				new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);

			String requestJson = mapper.writeValueAsString(Map.of(
				"title", params.get("title"),
				"description", params.get("description"),
				"capacity", Integer.valueOf(params.get("capacity")),
				"duration", Integer.valueOf(params.get("duration")),
				"videoUrl", params.get("videoUrl"),
				"imageFileName", params.get("imageFileName"),
				"contentType", params.get("contentType")
			));

			ResultActions resultActions = mvc.perform(
				post(baseRoomApi)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			resultActions
				.andExpect(handler().handlerType(ChallengeRoomController.class))
				.andExpect(handler().methodName("createRoom"))
				.andExpect(jsonPath("$.resultCode").value(ChallengeRoomHttp.CREATE_ROOM_SUCCESS_CODE))
				.andExpect(jsonPath("$.msg").value(ChallengeRoomHttp.CREATE_ROOM_SUCCESS_MESSAGE));

			MvcResult mvcResult = resultActions.andReturn();

			String responseJson = mvcResult.getResponse().getContentAsString();
			String title = JsonPath.read(responseJson, "$.data.title");
			String image = JsonPath.read(responseJson, "$.data.roomImage");
			long id = JsonPath.<Number>read(responseJson, "$.data.id").longValue();

			ChallengeRoom createdRoom = challengeRoomRepository.findById(id).orElseThrow(() ->
				new BusinessException(CommonErrorCode.NOT_FOUND)
			);

			assertThat(title).isEqualTo(params.get("title"));
			assertThat(createdRoom.getRoomImage()).isEqualTo(image);
		}

		@Test
		@DisplayName("Failed with NOT FOUND USER")
		void notFoundUserId() throws Exception {
			Map<String, String> params = createRoomRequestBuilder.toParamMap();

			Long wrongUserId = user.getId() + 1L;
			var authorities = AuthorityUtils.createAuthorityList("ROLE");
			securityUser = new SecurityUser(wrongUserId, user.getPassword(), user.getNickname(), authorities);
			authentication =
				new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);

			String requestJson = mapper.writeValueAsString(Map.of(
				"title", params.get("title"),
				"description", params.get("description"),
				"capacity", Integer.valueOf(params.get("capacity")),
				"duration", Integer.valueOf(params.get("duration")),
				"videoUrl", params.get("videoUrl"),
				"imageFileName", params.get("imageFileName"),
				"contentType", params.get("contentType")
			));

			ResultActions resultActions = mvc.perform(
				post(baseRoomApi)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
			).andDo(print());

			ChallengeRoomErrorCode error = ChallengeRoomErrorCode.NOT_FOUND_USER;

			resultActions
				.andExpect(handler().handlerType(ChallengeRoomController.class))
				.andExpect(handler().methodName("createRoom"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.resultCode").value(error.getCode()))
				.andExpect(jsonPath("$.msg").value(error.getMessage()));
		}

		@Test
		@DisplayName("Auto Join as Host after Create Room")
		void autoJoinAsHostAfterCreateRoom() throws Exception {
			// given
			User owner = userHelper.createUser();
			Map<String, String> params = createRoomRequestBuilder.toParamMap();

			var authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
			securityUser = new SecurityUser(owner.getId(), owner.getPassword(), owner.getNickname(), authorities);
			authentication =
				new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);

			String requestJson = mapper.writeValueAsString(Map.of(
				"title", params.get("title"),
				"description", params.get("description"),
				"capacity", Integer.valueOf(params.get("capacity")),
				"duration", Integer.valueOf(params.get("duration")),
				"videoUrl", params.get("videoUrl"),
				"imageFileName", params.get("imageFileName"),
				"contentType", params.get("contentType")
			));

			// when
			MvcResult result = mvc.perform(post(baseRoomApi)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson)
				)
				.andExpect(jsonPath("$.resultCode").value(ChallengeRoomHttp.CREATE_ROOM_SUCCESS_CODE))
				.andExpect(jsonPath("$.msg").value(ChallengeRoomHttp.CREATE_ROOM_SUCCESS_MESSAGE))
				.andReturn();

			// then
			String responseJson = result.getResponse().getContentAsString();
			Long roomId = JsonPath.<Number>read(responseJson, "$.data.id").longValue();
			ChallengeRoom createdRoom = challengeRoomRepository.findById(roomId)
				.orElseThrow();
			ChallengeParticipant participant = challengeParticipantRepository
				.findByUserAndChallengeRoom(owner, createdRoom)
				.orElseThrow();

			assertThat(participant.getRole()).isEqualTo(ChallengeParticipantRole.HOST);
		}
	}

	@Nested
	@DisplayName("GET `/api/v1/challenge/rooms` - 운동방 전체 목록 조회")
	class GetRoomsTest {
		private String baseRoomApi = "/api/v1/challenge/rooms";

		@Test
		@DisplayName("운동방 목록 조회 성공, page=0 & size=20")
		void successGetRoomsWithParams() throws Exception {
			int page = 0;
			int size = 20;

			for (int i = 0; i < size + 1; i++) {
				roomHelper.createChallengeRoom(user);
			}

			var authorities = AuthorityUtils.createAuthorityList("ROLE");
			securityUser = new SecurityUser(user.getId(), user.getPassword(), user.getNickname(), authorities);
			authentication =
				new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);

			ResultActions resultActions = mvc.perform(
				get(baseRoomApi)
					.param("page", Integer.toString(page))
					.param("size", Integer.toString(size))
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(handler().handlerType(ChallengeRoomController.class))
				.andExpect(handler().methodName("getRooms"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultCode").value(ChallengeRoomHttp.GET_ROOMS_SUCCESS_CODE))
				.andExpect(jsonPath("$.msg").value(ChallengeRoomHttp.GET_ROOMS_SUCCESS_MESSAGE));

			MvcResult mvcResult = resultActions.andReturn();
			String responseJson = mvcResult.getResponse().getContentAsString();
			Object raw = JsonPath.read(responseJson, "$.data");
			GetRoomsResponse data = mapper.convertValue(raw,
				new TypeReference<GetRoomsResponse>() {
				}
			);

			assertThat(data.getRooms()).hasSizeLessThanOrEqualTo(size);
		}

		@Test
		@DisplayName("운동방 목록 조회 성공, default 쿼리 params")
		void successGetRoomsWithDefault() throws Exception {
			for (int i = 0; i < 20; i++) {
				roomHelper.createChallengeRoom(user);
			}

			var authorities = AuthorityUtils.createAuthorityList("ROLE");
			securityUser = new SecurityUser(user.getId(), user.getPassword(), user.getNickname(), authorities);
			authentication =
				new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);

			ResultActions resultActions = mvc.perform(
				get(baseRoomApi)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(handler().handlerType(ChallengeRoomController.class))
				.andExpect(handler().methodName("getRooms"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultCode").value(ChallengeRoomHttp.GET_ROOMS_SUCCESS_CODE))
				.andExpect(jsonPath("$.msg").value(ChallengeRoomHttp.GET_ROOMS_SUCCESS_MESSAGE));

			MvcResult mvcResult = resultActions.andReturn();
			String responseJson = mvcResult.getResponse().getContentAsString();
			Object raw = JsonPath.read(responseJson, "$.data");
			GetRoomsResponse data = mapper.convertValue(raw,
				new TypeReference<GetRoomsResponse>() {
				}
			);

			assertThat(data.getRooms()).hasSizeLessThanOrEqualTo(Integer.parseInt(ChallengeRoomConstants.DEFAULT_SIZE));
		}
	}

	@Nested
	@DisplayName("GET `/api/v1/challenge/rooms/{roomId}` - 운동방 상세 조회")
	class GetRoomTest {
		private String getRoomApi = "/api/v1/challenge/rooms/{roomId}";

		@Test
		@DisplayName("운동방 조회 성공")
		void successGetRoom() throws Exception {
			ChallengeRoom room = roomHelper.createChallengeRoom(user);

			ResultActions resultActions = mvc.perform(
				get(getRoomApi, room.getId())
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(handler().handlerType(ChallengeRoomController.class))
				.andExpect(handler().methodName("getRoom"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultCode").value(ChallengeRoomHttp.GET_ROOM_SUCCESS_CODE))
				.andExpect(jsonPath("$.msg").value(ChallengeRoomHttp.GET_ROOM_SUCCESS_MESSAGE))
				.andExpect(jsonPath("$.data.id").value(room.getId()));
		}

		@Test
		@DisplayName("운동방 조회 실패 - 잘못된 roomId")
		void failedGetRoomWithWrongId() throws Exception {
			ChallengeRoom room = roomHelper.createChallengeRoom(user);

			ResultActions resultActions = mvc.perform(
				get(getRoomApi, room.getId() + 1)
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			ChallengeRoomErrorCode error = ChallengeRoomErrorCode.NOT_FOUND_ROOM;

			resultActions
				.andExpect(handler().handlerType(ChallengeRoomController.class))
				.andExpect(handler().methodName("getRoom"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.resultCode").value(error.getCode()))
				.andExpect(jsonPath("$.msg").value(error.getMessage()));
		}
	}

	@Nested
	@DisplayName("DELETE `/api/v1/challenge/rooms/{roomId}` - 운동방 삭제")
	class DeleteRoomTest {
		private String deleteRoomApi = "/api/v1/challenge/rooms/{roomId}";

		@Test
		@DisplayName("운동방 삭제 성공")
		void successDeleteRoom() throws Exception {
			var authorities = AuthorityUtils.createAuthorityList("ROLE");
			securityUser = new SecurityUser(user.getId(), user.getPassword(), user.getNickname(), authorities);
			authentication =
				new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);

			ChallengeRoom room = roomHelper.createChallengeRoom(user);
			participantHelper.createHostParticipant(user, room);

			ResultActions resultActions = mvc.perform(
				delete(deleteRoomApi, room.getId())
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(handler().handlerType(ChallengeRoomController.class))
				.andExpect(handler().methodName("deleteRoom"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultCode").value(ChallengeRoomHttp.DELETE_ROOM_SUCCESS_CODE))
				.andExpect(jsonPath("$.msg").value(ChallengeRoomHttp.DELETE_ROOM_SUCCESS_MESSAGE));

			assertThat(challengeRoomRepository.findById(room.getId())).isEmpty();

			LocalDateTime deletedAt = challengeRoomRepository.findDeletedAtRaw(room.getId());
			assertThat(deletedAt).isNotNull();
		}

		@Test
		@DisplayName("운동방 삭제 실패 - 일반 참여자 권한 거부")
		void failedDeleteRoomWithRole() throws Exception {
			var authorities = AuthorityUtils.createAuthorityList("ROLE");
			securityUser = new SecurityUser(user.getId(), user.getPassword(), user.getNickname(), authorities);
			authentication =
				new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);

			ChallengeRoom room = roomHelper.createChallengeRoom(user);
			participantHelper.createNormalParticipant(user, room);

			ResultActions resultActions = mvc.perform(
				delete(deleteRoomApi, room.getId())
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			ChallengeRoomErrorCode error = ChallengeRoomErrorCode.INVALID_AUTH_USER;

			resultActions
				.andExpect(handler().handlerType(ChallengeRoomController.class))
				.andExpect(handler().methodName("deleteRoom"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.resultCode").value(error.getCode()))
				.andExpect(jsonPath("$.msg").value(error.getMessage()));

		}
	}

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}
}
