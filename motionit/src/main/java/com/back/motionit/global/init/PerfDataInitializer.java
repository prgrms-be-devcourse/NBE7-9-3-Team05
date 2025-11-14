package com.back.motionit.global.init;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.back.motionit.domain.challenge.comment.entity.Comment;
import com.back.motionit.domain.challenge.comment.repository.CommentRepository;
import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus;
import com.back.motionit.domain.challenge.mission.repository.ChallengeMissionStatusRepository;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole;
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo;
import com.back.motionit.domain.challenge.video.entity.OpenStatus;
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository;
import com.back.motionit.domain.user.entity.LoginType;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@Profile("perf")
@RequiredArgsConstructor
public class PerfDataInitializer {

	private final UserRepository userRepository;
	private final ChallengeRoomRepository challengeRoomRepository;
	private final ChallengeParticipantRepository challengeParticipantRepository;
	private final ChallengeVideoRepository challengeVideoRepository;
	private final ChallengeMissionStatusRepository challengeMissionStatusRepository;
	private final CommentRepository commentRepository;

	@Bean
	public ApplicationRunner initPerfDummyData() {
		return args -> {
			if (challengeRoomRepository.count() > 0) {
				log.info("✅ perf 데이터 이미 존재. 초기화 스킵");
				return;
			}

			log.info("🚀 perf 프로파일용 더미 데이터 생성 시작");

			// 1️⃣ host 생성
			User host = userRepository.save(User.builder()
				.kakaoId(9001L)
				.email("perf_host@example.com")
				.nickname("PerfHost")
				.password("1234")
				.loginType(LoginType.KAKAO)
				.userProfile("https://picsum.photos/100?perf1")
				.build());

			// 2️⃣ 일반 유저 200명 생성
			List<User> users = IntStream.rangeClosed(1, 200)
				.mapToObj(i -> userRepository.save(User.builder()
					.kakaoId(9100L + i)
					.email("perf_user" + i + "@example.com")
					.nickname("PerfUser" + i)
					.password("pass" + i)
					.loginType(LoginType.KAKAO)
					.userProfile("https://picsum.photos/100?perf" + (i + 1))
					.build()))
				.toList();

			// 3️⃣ 방 15개 생성 (host가 전부 소유)
			List<ChallengeRoom> rooms = IntStream.rangeClosed(1, 15)
				.mapToObj(idx -> challengeRoomRepository.save(new ChallengeRoom(
					host,
					"🔥 K6 부하테스트 전용 방 #" + idx,
					"부하테스트용 방입니다. (" + idx + ")",
					100,
					OpenStatus.OPEN,
					LocalDateTime.now().minusDays(1),
					LocalDateTime.now().plusDays(7),
					"images/test/perf_room_" + idx + ".png",
					null
				)))
				.toList();

			// 4️⃣ 각 방에 host 참가자 추가
			Map<Long, List<ChallengeParticipant>> roomParticipantsMap = new HashMap<>();
			for (ChallengeRoom r : rooms) {
				ChallengeParticipant hostParticipant = challengeParticipantRepository.save(
					ChallengeParticipant.builder()
						.user(host)
						.challengeRoom(r)
						.role(ChallengeParticipantRole.HOST)
						.quited(false)
						.challengeStatus(false)
						.build()
				);
				roomParticipantsMap.put(r.getId(), new ArrayList<>(List.of(hostParticipant)));
			}

			// 5️⃣ 유저 → 방 매핑 (고정 규칙: (userId-1)%15)
			List<ChallengeParticipant> participants = new ArrayList<>();
			for (User u : users) {
				int roomIndex = ((u.getId().intValue() - 1) % rooms.size());
				ChallengeRoom assigned = rooms.get(roomIndex);

				ChallengeParticipant p = ChallengeParticipant.builder()
					.user(u)
					.challengeRoom(assigned)
					.role(ChallengeParticipantRole.NORMAL)
					.quited(false)
					.challengeStatus(false)
					.build();

				participants.add(p);
				roomParticipantsMap.computeIfAbsent(assigned.getId(), k -> new ArrayList<>()).add(p);
			}
			challengeParticipantRepository.saveAll(participants);

			// 기존 방(1~15) 이후 ID로 이어질 테스트용 빈 방 200개 생성 (join 부하테스트용)
			List<ChallengeRoom> extraRooms = IntStream.rangeClosed(1, 200)
				.mapToObj(i -> challengeRoomRepository.save(new ChallengeRoom(
					host,
					"Join 부하테스트 전용 방 #" + (15 + i), // 실제 ID는 16~215로 생성될 예정
					"성능 측정을 위한 빈 챌린지 방입니다. (join 테스트용 #" + (15 + i) + ")",
					100,
					OpenStatus.OPEN,
					LocalDateTime.now().minusDays(1),
					LocalDateTime.now().plusDays(7),
					"images/test/perf_room_extra_" + (15 + i) + ".png",
					null
				)))
				.toList();

			// 6️⃣ 오늘의 영상 1개씩 생성
			List<ChallengeVideo> todayVideos = rooms.stream()
				.map(r -> challengeVideoRepository.save(ChallengeVideo.builder()
					.challengeRoom(r)
					.user(host)
					.youtubeVideoId("2fpek3wzSZo")
					.title("오늘의 퍼포먼스 테스트 영상 - Room " + r.getId())
					.thumbnailUrl("https://i.ytimg.com/vi/2fpek3wzSZo/hqdefault.jpg")
					.duration(3528)
					.uploadDate(LocalDate.now())
					.isTodayMission(true)
					.build()))
				.toList();

			// 7️⃣ 오늘 미션 상태 (전원)
			List<ChallengeMissionStatus> missions = new ArrayList<>();
			for (List<ChallengeParticipant> plist : roomParticipantsMap.values()) {
				for (ChallengeParticipant p : plist) {
					missions.add(new ChallengeMissionStatus(p, LocalDate.now()));
				}
			}
			challengeMissionStatusRepository.saveAll(missions);

			// 8️⃣ 각 방에 댓글 50개 생성
			List<Comment> seedComments = new ArrayList<>();
			ThreadLocalRandom rnd = ThreadLocalRandom.current();

			for (ChallengeRoom r : rooms) {
				List<ChallengeParticipant> plist = roomParticipantsMap.getOrDefault(r.getId(), List.of());
				if (plist.isEmpty())
					continue;

				for (int i = 1; i <= 50; i++) {
					ChallengeParticipant writer = plist.get(rnd.nextInt(plist.size()));
					User author = writer.getUser();

					seedComments.add(Comment.builder()
						.challengeRoom(r)
						.user(author)
						.content("Perf seed comment #" + i + " in room " + r.getId() + " by " + author.getNickname())
						.build());
				}
			}
			commentRepository.saveAll(seedComments);

			log.info("🎯 perf 더미데이터 생성 완료! rooms={}, users={}, comments={}, videos={}",
				rooms.size(), users.size(), seedComments.size(), todayVideos.size());
		};
	}
}