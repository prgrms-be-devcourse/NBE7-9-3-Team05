package com.back.motionit.domain.challenge.room.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole;
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;
import com.back.motionit.domain.challenge.participant.service.ChallengeParticipantService;
import com.back.motionit.domain.challenge.room.dto.ChallengeParticipantDto;
import com.back.motionit.domain.challenge.room.dto.ChallengeVideoDto;
import com.back.motionit.domain.challenge.room.dto.CreateRoomRequest;
import com.back.motionit.domain.challenge.room.dto.CreateRoomResponse;
import com.back.motionit.domain.challenge.room.dto.GetRoomResponse;
import com.back.motionit.domain.challenge.room.dto.GetRoomSummary;
import com.back.motionit.domain.challenge.room.dto.GetRoomsResponse;
import com.back.motionit.domain.challenge.room.dto.RoomEventDto;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomSummaryRepository;
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo;
import com.back.motionit.domain.challenge.video.entity.OpenStatus;
import com.back.motionit.domain.challenge.video.service.ChallengeVideoService;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.enums.ChallengeStatus;
import com.back.motionit.global.enums.EventEnums;
import com.back.motionit.global.error.code.ChallengeRoomErrorCode;
import com.back.motionit.global.error.exception.BusinessException;
import com.back.motionit.global.event.EventPublisher;
import com.back.motionit.global.service.AwsS3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeRoomService {

	private final ChallengeRoomRepository challengeRoomRepository;
	private final ChallengeParticipantService challengeParticipantService;
	private final ObjectProvider<AwsS3Service> s3Provider;
	private final EventPublisher eventPublisher;
	private final UserRepository userRepository;
	private final ChallengeParticipantRepository participantRepository;
	private final ChallengeParticipantService participantService;
	private final ChallengeRoomSummaryRepository summaryRepository;
	private final ChallengeVideoService videoService;

	private AwsS3Service s3() {
		return s3Provider.getIfAvailable();
	}

	@Transactional
	public CreateRoomResponse createRoom(CreateRoomRequest input, User user) {
		if (user == null) {
			throw new BusinessException(ChallengeRoomErrorCode.NOT_FOUND_USER);
		}

		Long userId = user.getId();

		User host = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ChallengeRoomErrorCode.NOT_FOUND_USER));

		String objectKey = "";
		String uploadUrl = "";

		AwsS3Service s3 = s3();
		if (s3 != null) {
			objectKey = s3.buildObjectKey(input.imageFileName());
		}

		ChallengeRoom room = mapToRoomObject(input, host, objectKey);
		ChallengeRoom createdRoom = challengeRoomRepository.save(room);

		// 방장 자동 참가 처리, 여기서 실패시 방 생성도 롤백 처리됨
		autoJoinAsHost(createdRoom);
		videoService.uploadChallengeVideo(userId, createdRoom.getId(), input.videoUrl());

		if (s3 != null && StringUtils.hasText(objectKey)) {
			uploadUrl = s3.createUploadUrl(objectKey, input.contentType());
		} else {
			uploadUrl = "";
		}

		CreateRoomResponse response = mapToCreateRoomResponse(createdRoom, uploadUrl);
		eventPublisher.publishEvent(new RoomEventDto(EventEnums.ROOM));

		return response;
	}

	@Transactional(readOnly = true)
	public GetRoomsResponse getRooms(User user, int page, int size) {
		Pageable pageable = PageRequest.of(
			page,
			size,
			Sort.by(Sort.Direction.DESC, "createDate")
		);

		Page<ChallengeRoom> pageResult = summaryRepository.fetchOpenRooms(pageable);
		List<ChallengeRoom> rooms = pageResult.getContent();

		if (rooms.isEmpty()) {
			return new GetRoomsResponse(countOpenRooms(), List.of());
		}

		List<Long> roomIds = rooms.stream().map(ChallengeRoom::getId).toList();
		Set<Long> joiningSet = (user == null) ? Set.<Long>of()
			: Set.copyOf(participantRepository.findJoiningRoomIdsByUserAndRoomIds(user.getId(), roomIds));

		List<Object[]> raw = participantRepository.countActiveParticipantsByRoomIds(roomIds);
		Map<Long, Integer> countMap = new HashMap<Long, Integer>(raw.size());

		for (Object[] row : raw) {
			Long roomId = (Long)row[0];
			Long count = (Long)row[1];
			countMap.put(roomId, count.intValue());
		}

		var summaries = rooms.stream().map(room -> new GetRoomSummary(
			room.getId(),
			room.getTitle(),
			room.getDescription(),
			room.getCapacity(),
			(int)ChronoUnit.DAYS.between(LocalDate.now(), room.getChallengeEndDate().toLocalDate()),
			room.getRoomImage(),
			(user != null && joiningSet.contains(room.getId())) ? ChallengeStatus.JOINING : ChallengeStatus.JOINABLE,
			countMap.getOrDefault(room.getId(), 0)
		)).toList();

		return new GetRoomsResponse((int)pageResult.getTotalElements(), summaries);
	}

	@Transactional
	public GetRoomResponse getRoom(Long roomId) {
		ChallengeRoom room = challengeRoomRepository.findWithVideosById(roomId).orElseThrow(
			() -> new BusinessException(ChallengeRoomErrorCode.NOT_FOUND_ROOM)
		);

		return mapToGetRoomResponse(room);
	}

	@Transactional
	public void deleteRoom(Long roomId, User user) {
		if (!participantService.checkParticipantIsRoomHost(user.getId(), roomId)) {
			throw new BusinessException(ChallengeRoomErrorCode.INVALID_AUTH_USER);
		}

		int deleted = challengeRoomRepository.softDeleteById(roomId);

		if (deleted == 0) {
			throw new BusinessException(ChallengeRoomErrorCode.FAILED_DELETE_ROOM);
		}

		eventPublisher.publishEvent(new RoomEventDto(EventEnums.ROOM));
	}

	public ChallengeRoom mapToRoomObject(CreateRoomRequest input, User user, String objectKey) {
		LocalDateTime now = LocalDateTime.now();
		int durationDays = input.duration();

		LocalDateTime start = now;
		LocalDateTime end = start.plusDays(durationDays);

		List<ChallengeVideo> videos = new ArrayList<>();
		List<ChallengeParticipant> participants = new ArrayList<>();

		return new ChallengeRoom(
			user,
			input.title(),
			input.description(),
			input.capacity(),
			OpenStatus.OPEN,
			start,
			end,
			objectKey,
			null,
			videos,
			participants
		);
	}

	private CreateRoomResponse mapToCreateRoomResponse(ChallengeRoom room, String uploadUrl) {
		return new CreateRoomResponse(
			room.getId(),
			room.getTitle(),
			room.getDescription(),
			room.getCapacity(),
			room.getOpenStatus(),
			room.getChallengeStartDate(),
			room.getChallengeEndDate(),
			room.getRoomImage(),
			room.getChallengeVideoList(),
			uploadUrl
		);
	}

	private GetRoomResponse mapToGetRoomResponse(ChallengeRoom room) {
		List<ChallengeVideoDto> videos = room.getChallengeVideoList().stream()
			.map(ChallengeVideoDto::new)
			.toList();

		List<ChallengeParticipantDto> participants = participantRepository.findAllByRoomIdWithUser(room.getId())
			.stream()
			.map(ChallengeParticipantDto::new)
			.toList();

		return new GetRoomResponse(
			room,
			videos,
			participants
		);
	}

	private void autoJoinAsHost(ChallengeRoom createdRoom) {
		challengeParticipantService.joinChallengeRoom(
			createdRoom.getUser().getId(),
			createdRoom.getId(),
			ChallengeParticipantRole.HOST
		);
	}

	public int countOpenRooms() {
		return summaryRepository.countOpenRooms();
	}
}
