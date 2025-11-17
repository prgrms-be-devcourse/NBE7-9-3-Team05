package com.back.motionit.domain.challenge.participant.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;

import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.challenge.video.entity.OpenStatus;
import com.back.motionit.domain.challenge.video.service.ChallengeVideoService;
import com.back.motionit.domain.user.entity.LoginType;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.event.EventPublisher;
import com.back.motionit.global.service.AwsCdnSignService;
import com.back.motionit.global.service.AwsS3Service;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // H2 사용
@Import(ChallengeParticipantService.class)
class ChallengeParticipantServiceConcurrencyTest {

	@Autowired
	private ChallengeParticipantService challengeParticipantService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ChallengeRoomRepository challengeRoomRepository;

	@Autowired
	private ChallengeParticipantRepository challengeParticipantRepository;

	@Autowired
	EntityManager em;

	@MockitoBean
	EventPublisher eventPublisher;

	@MockitoBean
	AwsS3Service s3Service;

	@MockitoBean
	AwsCdnSignService cdnSignService;

	@MockitoBean
	ChallengeVideoService challengeVideoService;

	@BeforeEach
	void setUp() {
		User admin = userRepository.saveAndFlush(User.builder()
			.email("admin@test.com")
			.nickname("운영자")
			.password("1234")
			.loginType(LoginType.LOCAL)
			.build());

		ChallengeRoom room = new ChallengeRoom(
			admin,
			"동시성 테스트 방",
			"테스트용 방입니다",
			5,
			OpenStatus.OPEN,
			LocalDateTime.now(),
			LocalDateTime.now().plusDays(7),
			"https://example.com/test-room.png",
			null
		);
		challengeRoomRepository.saveAndFlush(room);

		for (int i = 1; i <= 10; i++) {
			userRepository.saveAndFlush(User.builder()
				.email("user" + i + "@test.com")
				.nickname("유저" + i)
				.password("1234")
				.loginType(LoginType.LOCAL)
				.build());
		}

		em.flush();
		em.clear();

		TestTransaction.flagForCommit();
		TestTransaction.end(); // 현재 테스트 트랜잭션 종료(커밋)
	}

	@Test
	@DisplayName("동시에 10명 참가시 정원초과 방지_테스트")
	void challengeParticipantConcurrencyTest() throws InterruptedException {
		ChallengeRoom room = challengeRoomRepository.findAll().get(0);
		List<User> users = userRepository.findAll();

		int threadCount = 10;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			final Long userId = users.get(i).getId();
			executorService.submit(() -> {
				try {
					challengeParticipantService.joinChallengeRoom(userId, room.getId());
				} catch (Exception e) {
					System.out.println("실패한 스레드: " + userId + " (" + e.getMessage() + ")");
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		int finalCount = challengeParticipantRepository.countByChallengeRoomAndQuitedFalse(room);
		System.out.println("최종 참가자 수: " + finalCount);

		Assertions.assertThat(finalCount).isEqualTo(room.getCapacity());
	}
}
