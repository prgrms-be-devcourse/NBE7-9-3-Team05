package com.back.motionit.domain.challenge.participant.service

import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.challenge.video.entity.OpenStatus
import com.back.motionit.domain.challenge.video.service.ChallengeVideoService
import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.config.JpaConfig
import com.back.motionit.global.event.EventPublisher
import com.back.motionit.global.service.AwsCdnSignService
import com.back.motionit.global.service.AwsS3Service
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.transaction.TestTransaction
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // H2 사용
@Import(ChallengeParticipantService::class, JpaConfig::class)
class ChallengeParticipantServiceConcurrencyTest {

    @Autowired
    lateinit var challengeParticipantService: ChallengeParticipantService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var challengeRoomRepository: ChallengeRoomRepository

    @Autowired
    lateinit var challengeParticipantRepository: ChallengeParticipantRepository

    @Autowired
    lateinit var em: EntityManager

    @MockitoBean
    lateinit var eventPublisher: EventPublisher

    @MockitoBean
    lateinit var s3Service: AwsS3Service

    @MockitoBean
    lateinit var cdnSignService: AwsCdnSignService

    @MockitoBean
    lateinit var challengeVideoService: ChallengeVideoService

    @BeforeEach
    fun setUp() {
        // 운영자 생성
        val admin = userRepository.saveAndFlush(
            User(
                email = "admin@test.com",
                nickname = "운영자",
                password = "1234",
                loginType = LoginType.LOCAL
            )
        )

        // 방 생성
        val room = ChallengeRoom(
            user = admin,
            title = "동시성 테스트 방",
            description = "테스트용 방입니다",
            capacity = 5,
            openStatus = OpenStatus.OPEN,
            challengeStartDate = LocalDateTime.now(),
            challengeEndDate = LocalDateTime.now().plusDays(7),
            roomImage = "https://example.com/test-room.png",
            deletedAt = null
        )
        challengeRoomRepository.saveAndFlush(room)

        // 10명 유저 생성
        for (i in 1..10) {
            userRepository.saveAndFlush(
                User(
                    email = "user$i@test.com",
                    nickname = "유저$i",
                    password = "1234",
                    loginType = LoginType.LOCAL
                )
            )
        }

        // 영속성 초기화
        em.flush()
        em.clear()

        // DataJpaTest는 기본 트랜잭션 안에서 실행되므로 강제 종료
        TestTransaction.flagForCommit()
        TestTransaction.end()
    }

    @Test
    @DisplayName("동시에 10명 참가 시 정원 초과 방지")
    fun challengeParticipantConcurrencyTest() {
        val room = challengeRoomRepository.findAll().first()
        val users = userRepository.findAll()

        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) { i ->
            val userId = users[i].id!!
            executor.submit {
                try {
                    challengeParticipantService.joinChallengeRoom(userId, room.id!!)
                } catch (e: Exception) {
                    println("실패한 스레드: $userId (${e.message})")
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        val finalCount = challengeParticipantRepository.countByChallengeRoomAndQuitedFalse(room)
        println("최종 참가자 수: $finalCount")

        assertThat(finalCount)
            .describedAs("정원 ${room.capacity}를 초과하면 안됨")
            .isEqualTo(room.capacity)
    }
}