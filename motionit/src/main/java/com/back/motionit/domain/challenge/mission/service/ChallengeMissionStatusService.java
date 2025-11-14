package com.back.motionit.domain.challenge.mission.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus;
import com.back.motionit.domain.challenge.mission.repository.ChallengeMissionStatusRepository;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.validator.ChallengeAuthValidator;
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository;
import com.back.motionit.global.error.code.ChallengeMissionErrorCode;
import com.back.motionit.global.error.exception.BusinessException;
import com.back.motionit.global.service.GptService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeMissionStatusService {

	private final ChallengeMissionStatusRepository challengeMissionStatusRepository;
	private final ChallengeParticipantRepository challengeParticipantRepository;
	private final ChallengeVideoRepository challengeVideoRepository;
	private final ChallengeAuthValidator challengeAuthValidator;
	private final ObjectProvider<GptService> gptProvider;

	@Transactional(readOnly = true)
	public String generateAiSummary(Long roomId, Long actorId) {
		ChallengeParticipant participant = challengeAuthValidator.validateActiveParticipant(actorId, roomId);
		ChallengeMissionStatus mission = getTodayMissionStatus(roomId, actorId);
		GptService gptService = gptProvider.getIfAvailable();

		// 이미 저장된 AI 메시지가 있으면 그대로 반환
		if (mission.getAiMessage() != null && !mission.getAiMessage().isEmpty()) {
			log.info("[AI Summary] Using cached AI message for user={}, room={}", actorId, roomId);
			return mission.getAiMessage();
		}

		if (gptService == null) {
			log.warn("[AI Summary] GPT disabled or not configured. user={}, room={}", actorId, roomId);
			return "응원 메시지를 준비중입니다"; // 테스트/비활성 환경 기본 응답
		}

		// 저장된 메시지가 없으면 새로 생성
		try {
			return gptService.generateMissionCompleteSummary(
				participant.getUser().getNickname(),
				participant.getChallengeRoom().getTitle()
			);
		} catch (Exception e) {
			log.error("[AI Summary] failed to generate summary for user={}, room={}", actorId, roomId, e);
			return "응원 메시지 생성에 실패했습니다";
		}
	}

	@Transactional
	public ChallengeMissionStatus completeMission(Long roomId, Long actorId) {
		// 참여중인 참가자인지 확인 - controller를 거치지 않은 호출에 대비
		ChallengeParticipant participant = challengeAuthValidator.validateActiveParticipant(actorId, roomId);
		GptService gptService = gptProvider.getIfAvailable();

		// 영상 존재 확인
		boolean hasTodayVideo = challengeVideoRepository
			.existsByChallengeRoomIdAndUploadDate(roomId, LocalDate.now());

		if (!hasTodayVideo) {
			throw new BusinessException(ChallengeMissionErrorCode.NO_VIDEO_UPLOADED);
		}
		LocalDate today = LocalDate.now();

		ChallengeMissionStatus mission = challengeMissionStatusRepository
			.findByParticipantIdAndMissionDate(participant.getId(), today);

		if (mission == null) {
			ChallengeMissionStatus newMission = ChallengeMissionStatus.create(participant, today);
			mission = challengeMissionStatusRepository.save(newMission);
		}

		// 이미 완료된 미션인지 확인
		if (Boolean.TRUE.equals(mission.getCompleted())) {
			throw new BusinessException(ChallengeMissionErrorCode.ALREADY_COMPLETED);
		}

		// 미션 완료 상태로 업데이트
		mission.completeMission();

		if (gptService == null) {
			log.warn("[AI Summary] GPT disabled or not configured. user={}, room={}", actorId, roomId);
		}

		if (mission.getAiMessage() == null || mission.getAiMessage().isEmpty()) {
			try {
				String aiMessage = gptService.generateMissionCompleteSummary(
					participant.getUser().getNickname(),
					participant.getChallengeRoom().getTitle()
				);
				mission.setAiMessage(aiMessage);
				log.info("[Mission Complete] AI message generated and saved for user={}, room={}", actorId, roomId);
			} catch (Exception e) {
				log.error("[Mission Complete] Failed to generate AI message for user={}, room={}", actorId, roomId, e);
				// AI 생성 실패해도 미션 완료는 진행
				mission.setAiMessage("응원 메시지 생성에 실패했습니다");
			}
		}

		return mission;
	}

	@Transactional(readOnly = true)
	public ChallengeMissionStatus getTodayMissionStatus(Long roomId, Long actorId) {
		ChallengeParticipant participant = challengeAuthValidator.validateActiveParticipant(actorId, roomId);

		LocalDate today = LocalDate.now();

		ChallengeMissionStatus mission =
			challengeMissionStatusRepository.findByParticipantIdAndMissionDate(
				participant.getId(), today
			);

		if (mission == null) {
			throw new BusinessException(ChallengeMissionErrorCode.NOT_INITIALIZED_MISSION);
		}

		return mission;
	}

	// 특정 운동방의 모든 참가자의 오늘 미션 상태 조회
	@Transactional(readOnly = true)
	public List<ChallengeMissionStatus> getTodayMissionsByRoom(Long roomId, Long actorId) {
		// 접근 권한 확인
		ChallengeParticipant participant = challengeAuthValidator.validateActiveParticipant(actorId, roomId);
		ChallengeRoom challengeRoom = participant.getChallengeRoom();

		LocalDate today = LocalDate.now();

		// TODO: 쿼리 최적화 필요 (LEFT JOIN)
		List<ChallengeParticipant> participants = challengeParticipantRepository.findAllByChallengeRoomAndQuitedFalse(
			challengeRoom);

		// 오늘 미션 완료자 조회 (참가자까지 fetch)
		List<ChallengeMissionStatus> missions =
			challengeMissionStatusRepository.findByRoomAndDate(challengeRoom, today);

		// participantId → mission 매핑
		Map<Long, ChallengeMissionStatus> missionMap = missions.stream()
			.filter(m -> m.getParticipant() != null) // null-safe
			.collect(Collectors.toMap(
				m -> m.getParticipant().getId(),
				m -> m,
				(a, b) -> a // 중복 키 방지
			));

		// 전체 참가자 기준 병합 (미완료자 포함)
		List<ChallengeMissionStatus> allStatuses = participants.stream()
			.map(p -> missionMap.getOrDefault(
				p.getId(),
				ChallengeMissionStatus.create(p, today) // 엔티티 생성자 이용 (participant 연결 명시)
			))
			.toList();

		if (allStatuses.isEmpty()) {
			log.warn("[getTodayMissionsByRoom] {} 날짜에 미션 데이터가 없습니다. (roomId={})", today, roomId);
		}
		return allStatuses;
	}

	// 참가자의 미션 수행 내역 조회
	@Transactional(readOnly = true)
	public List<ChallengeMissionStatus> getMissionHistory(Long roomId, Long actorId) {
		ChallengeParticipant participant = challengeAuthValidator.validateActiveParticipant(actorId, roomId);

		return challengeMissionStatusRepository.findAllByParticipantId(participant.getId());
	}
}
