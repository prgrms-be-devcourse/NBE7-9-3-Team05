package com.back.motionit.domain.outbox

import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.challenge.video.dto.YoutubeVideoPayload
import com.back.motionit.domain.challenge.video.external.youtube.YoutubeMetadataResilientClient
import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository
import com.back.motionit.domain.challenge.video.service.ChallengeVideoService
import com.back.motionit.domain.user.entity.User
import com.back.motionit.factory.ChallengeParticipantFactory
import com.back.motionit.factory.ChallengeRoomFactory
import com.back.motionit.global.enums.OutboxEventType
import com.back.motionit.global.enums.OutboxStatus
import com.back.motionit.global.outbox.entity.OutboxEvent
import com.back.motionit.global.outbox.processor.OutboxProcessor
import com.back.motionit.global.outbox.repository.OutboxEventRepository
import com.back.motionit.helper.UserHelper
import com.back.motionit.security.SecurityUser
import com.back.motionit.support.BaseIntegrationTest
import com.back.motionit.support.SecuredIntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SecuredIntegrationTest
class OutboxProcessorIntegrationTest : BaseIntegrationTest() {
    @Autowired
    lateinit var outboxProcessor: OutboxProcessor

    @Autowired
    lateinit var outboxEventRepository: OutboxEventRepository

    @Autowired
    lateinit var userHelper: UserHelper

    @MockitoBean
    lateinit var challengeVideoService: ChallengeVideoService

    @Autowired
    lateinit var challengeVideoRepository: ChallengeVideoRepository

    @Autowired
    lateinit var challengeRoomRepository: ChallengeRoomRepository

    @Autowired
    lateinit var challengeParticipantRepository: ChallengeParticipantRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var youtubeMetadataResilientClient: YoutubeMetadataResilientClient

    private lateinit var user: User
    private lateinit var room: ChallengeRoom

    private lateinit var securityUser: SecurityUser
    private lateinit var authentication: UsernamePasswordAuthenticationToken
    private val youtubeUrl = "https://www.youtube.com/watch?v=F-Jd4kI6rdM"

    @BeforeEach
    fun setUp() {
        challengeVideoRepository.deleteAll()
        outboxEventRepository.deleteAll()

        // 기본 사용자 및 방, 참가자 세팅
        user = userHelper.createUser()
        room = challengeRoomRepository.save(ChallengeRoomFactory.fakeChallengeRoom(user, 5))
        challengeParticipantRepository.save(
            ChallengeParticipantFactory.fakeParticipant(user, room)
        )

        // 인증 세팅 (ChallengeRoomControllerTest와 동일)
        val authorities = AuthorityUtils.createAuthorityList("ROLE_USER")
        securityUser = SecurityUser(user.id!!, user.password!!, user.nickname, authorities)
        authentication =
            UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Nested
    @DisplayName("YOUTUBE_VIDEO Outbox Event 처리")
    inner class YoutubeVideoOutbox {
        @Test
        @DisplayName("성공 케이스: PENDING → COMPLETED, 메타데이터 fetch + saveChallengeVideo 호출")
        fun processYoutubeVideoEvent_success() {
            // given
            val payload = YoutubeVideoPayload(
                userId = user.id!!,
                roomId = room.id!!,
                youtubeUrl = youtubeUrl,
            )

            val event = outboxEventRepository.save(
                OutboxEvent(
                    eventType = OutboxEventType.YOUTUBE_VIDEO,
                    aggregateType = "ChallengeVideo",
                    aggregateId = room.id!!,
                    payload = objectMapper.writeValueAsString(payload),
                )
            )

            val fakeMetadata = YoutubeVideoMetadata(
                videoId = "VIDEO123",
                title = "Test Video Title",
                thumbnailUrl = "http://www.test.com",
                durationSeconds = 10,
            )

            `when`(youtubeMetadataResilientClient.fetchMetadata(youtubeUrl))
                .thenReturn(fakeMetadata)

            // when
            outboxProcessor.pollAndProcess()

            // then
            val updated = outboxEventRepository.findById(event.id!!).get()

            assertThat(updated.status).isEqualTo(OutboxStatus.COMPLETED)
            assertThat(updated.attemptCount).isEqualTo(1)
            assertThat(updated.lastErrorMessage).isNull()

            verify(youtubeMetadataResilientClient, times(1))
                .fetchMetadata(payload.youtubeUrl)

            verify(challengeVideoService, times(1))
                .saveChallengeVideo(
                    payload.userId,
                    payload.roomId,
                    fakeMetadata,
                    true,
                )
        }

        @Test
        @DisplayName("실패 케이스: 외부 HTTP 실패 → status=PENDING 유지, attemptCount=1")
        fun processYoutubeVideoEvent_failure_retryPending() {
            // given
            val badUrl = "${youtubeUrl}WRONG_URL"

            val payload = YoutubeVideoPayload(
                userId = user.id!!,
                roomId = room.id!!,
                youtubeUrl = badUrl,
            )

            val event = outboxEventRepository.save(
                OutboxEvent(
                    eventType = OutboxEventType.YOUTUBE_VIDEO,
                    aggregateType = "ChallengeVideo",
                    aggregateId = room.id!!,
                    payload = objectMapper.writeValueAsString(payload),
                )
            )

            `when`(youtubeMetadataResilientClient.fetchMetadata(badUrl))
                .thenThrow(RuntimeException("youtube api error"))

            // when
            outboxProcessor.pollAndProcess()

            // then
            val updated = outboxEventRepository.findById(event.id!!).get()

            assertThat(updated.status).isEqualTo(OutboxStatus.PENDING)
            assertThat(updated.attemptCount).isEqualTo(1)
            assertThat(updated.lastErrorMessage).isNotNull()

            verify(youtubeMetadataResilientClient, times(1))
                .fetchMetadata(badUrl)

            verifyNoInteractions(challengeVideoService)
        }

        @Test
        @DisplayName("5회 이상 실패하면 FAILED로 전환")
        fun processYoutubeVideoEvent_failureToFailedAfterMaxAttempts() {
            // given
            val badUrl = "${youtubeUrl}WRONG_URL"

            val payload = YoutubeVideoPayload(
                userId = user.id!!,
                roomId = room.id!!,
                youtubeUrl = badUrl,
            )

            val event = outboxEventRepository.save(
                OutboxEvent(
                    eventType = OutboxEventType.YOUTUBE_VIDEO,
                    aggregateType = "ChallengeVideo",
                    aggregateId = room.id!!,
                    payload = objectMapper.writeValueAsString(payload),
                    // 이미 4번 시도했다고 가정
                    status = OutboxStatus.PENDING,
                    attemptCount = 4,
                )
            )

            `when`(youtubeMetadataResilientClient.fetchMetadata(badUrl))
                .thenThrow(RuntimeException("permanent error"))

            // when
            outboxProcessor.pollAndProcess()

            // then
            val updated = outboxEventRepository.findById(event.id!!).get()

            assertThat(updated.attemptCount).isEqualTo(5)
            assertThat(updated.status).isEqualTo(OutboxStatus.FAILED)
            assertThat(updated.lastErrorMessage).isNotBlank()

            verify(youtubeMetadataResilientClient, times(1))
                .fetchMetadata(badUrl)

            verifyNoInteractions(challengeVideoService)
        }

        @Test
        @DisplayName("서킷 OPEN인 경우: CallNotPermittedException → status=PENDING 유지, attemptCount 그대로")
        fun processYoutubeVideoEvent_circuitOpen() {
            // given
            val circuitBreakerConfig = CircuitBreakerConfig.ofDefaults()
            val circuitBreaker = CircuitBreaker.of("youtubeMetadata", circuitBreakerConfig)

            val payload = YoutubeVideoPayload(
                userId = user.id!!,
                roomId = room.id!!,
                youtubeUrl = youtubeUrl,
            )

            val event = outboxEventRepository.save(
                OutboxEvent(
                    eventType = OutboxEventType.YOUTUBE_VIDEO,
                    aggregateType = "ChallengeVideo",
                    aggregateId = room.id!!,
                    payload = objectMapper.writeValueAsString(payload),
                )
            )

            `when`(youtubeMetadataResilientClient.fetchMetadata(youtubeUrl))
                .thenThrow(CallNotPermittedException.createCallNotPermittedException(circuitBreaker))

            // when
            outboxProcessor.pollAndProcess()

            // then
            val updated = outboxEventRepository.findById(event.id!!).get()

            assertThat(updated.status).isEqualTo(OutboxStatus.PENDING)

            verify(youtubeMetadataResilientClient, times(1)).fetchMetadata(youtubeUrl)
            verifyNoInteractions(challengeVideoService)
        }
    }
}
