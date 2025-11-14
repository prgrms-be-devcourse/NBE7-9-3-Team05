package com.back.motionit.domain.challenge.video.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.challenge.validator.ChallengeAuthValidator;
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo;
import com.back.motionit.domain.challenge.video.external.youtube.YoutubeMetadataClient;
import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata;
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.error.code.ChallengeVideoErrorCode;
import com.back.motionit.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeVideoService {

	private final ChallengeVideoRepository challengeVideoRepository;
	private final ChallengeRoomRepository challengeRoomRepository;
	private final ChallengeParticipantRepository challengeParticipantRepository;
	private final UserRepository userRepository;
	private final YoutubeMetadataClient youtubeMetadataClient; // 유튜브 메타데이터 클라이언트
	private final ChallengeAuthValidator challengeAuthValidator;

	@Transactional
	public ChallengeVideo uploadChallengeVideo(Long actorId, Long roomId, String youtubeUrl) {
		User user = getUserOrThrow(actorId);
		var participant = challengeAuthValidator.validateActiveParticipantWithRoom(actorId, roomId);
		var challengeRoom = participant.getChallengeRoom();

		YoutubeVideoMetadata metadata = fetchMetadata(youtubeUrl);
		validateDuplicateVideo(challengeRoom, metadata.getVideoId());

		ChallengeVideo video = ChallengeVideo.of(challengeRoom, user, metadata, true);
		return challengeVideoRepository.save(video);
	}

	// 오늘 업로드된 모든 '오늘의 미션 영상' 조회 (방 전체 기준)
	@Transactional(readOnly = true)
	public List<ChallengeVideo> getTodayMissionVideos(Long actorId, Long roomId) {
		challengeAuthValidator.validateActiveParticipant(actorId, roomId);
		ChallengeRoom challengeRoom = getRoomOrThrow(roomId);

		// 방에 업로드된 영상들 중에서 오늘의 미션 영상만 필터링
		// Querydsl 사용 / 명시적 JPQL 사용 대신 자바 스트림으로 필터링 -> 직관성
		return challengeVideoRepository.findByChallengeRoomId(roomId).stream()
			.filter(v -> v.isTodayMission() && v.getUploadDate().isEqual(LocalDate.now()))
			.toList();
	}

	//사용자가 직접 업로드한 영상 삭제
	@Transactional
	public void deleteVideoByUser(Long actorId, Long roomId, Long videoId) {
		challengeAuthValidator.validateActiveParticipant(actorId, roomId);
		ChallengeVideo video = challengeVideoRepository.findByIdAndUserId(videoId, actorId);

		if(video == null) {
			throw new BusinessException(ChallengeVideoErrorCode.VIDEO_NOT_FOUND_OR_FORBIDDEN);
		}

		challengeVideoRepository.delete(video);
	}

	// 특정 사용자가 오늘 업로드한 영상 목록 조회
	@Transactional(readOnly = true)
	public List<ChallengeVideo> getTodayVideosByUser(Long actorId) {
		LocalDate today = LocalDate.now();
		return challengeVideoRepository.findByUserIdAndUploadDate(actorId, today);
	}

	/**
	 * TODO:
	 *  * 현재 유저/방/참가자 검증 과정에서 DB 조회가 3~4회 발생함. -> 현재는 무결성에 초점
	 *  * 향후 트래픽 증가 시 아래 방향으로 리팩터링 고려:
	 *  * 1. ChallengeParticipantRepository.findByUserIdAndRoomIdFetchJoin() 으로 통합
	 *  * 2. RequestContext 내부에서 인증 유저 엔티티 캐싱
	 *  * 3. QueryDSL exists() 서브쿼리 활용으로 중복 조회 제거
	 */
	private User getUserOrThrow(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ChallengeVideoErrorCode.NOT_FOUND_USER));
	}

	private ChallengeRoom getRoomOrThrow(Long roomId) {
		return challengeRoomRepository.findById(roomId)
			.orElseThrow(() -> new BusinessException(ChallengeVideoErrorCode.CANNOT_FIND_CHALLENGE_ROOM));
	}

	private YoutubeVideoMetadata fetchMetadata(String youtubeUrl) {
		return youtubeMetadataClient.fetchMetadata(youtubeUrl);
	}

	private void validateDuplicateVideo(ChallengeRoom room, String videoId) {
		if (challengeVideoRepository.existsByChallengeRoomAndYoutubeVideoId(room, videoId)) {
			throw new BusinessException(ChallengeVideoErrorCode.DUPLICATE_VIDEO_IN_ROOM);
		}
	}
}
