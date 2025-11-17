package com.back.motionit.domain.challenge.video

import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.video.dto.YoutubeVideoPayload
import com.back.motionit.domain.challenge.video.external.youtube.YoutubeMetadataClient
import com.back.motionit.domain.challenge.video.service.ChallengeVideoService
import com.back.motionit.factory.ChallengeParticipantFactory
import com.back.motionit.global.enums.OutboxEventType
import com.back.motionit.global.enums.OutboxStatus
import com.back.motionit.global.outbox.repository.OutboxEventRepository
import com.back.motionit.helper.ChallengeRoomHelper
import com.back.motionit.helper.UserHelper
import com.back.motionit.support.BaseIntegrationTest
import com.back.motionit.support.SecuredIntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SecuredIntegrationTest
class VideoOutboxIntegrationTest: BaseIntegrationTest() {
    @Autowired
    lateinit var videoService: ChallengeVideoService

    @Autowired
    lateinit var outboxEventRepository: OutboxEventRepository

    @Autowired
    lateinit var userHelper: UserHelper

    @Autowired
    lateinit var roomHelper: ChallengeRoomHelper

    @Autowired
    lateinit var participantRepository: ChallengeParticipantRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var youtubeMetadataClient: YoutubeMetadataClient

    @Test
    @DisplayName("Youtube Video Outbox Test: Create new outbox event - should be success")
    fun createNewVideoOutboxEvent() {
        // given
        val user = userHelper.createUser()
        val room = roomHelper.createChallengeRoom(user)
        val youtubeUrl = "https://www.youtube.com/watch?v=F-Jd4kI6rdM"
        participantRepository.save(
            ChallengeParticipantFactory.fakeParticipant(user, room)
        )

        // when
        videoService.requestUploadChallengeVideo(user.id!!, room.id!!, youtubeUrl)

        // then
        val events = outboxEventRepository.findAll()
        assertThat(events).hasSize(1)

        val event = events[0]
        assertThat(event.eventType).isEqualTo(OutboxEventType.YOUTUBE_VIDEO)
        assertThat(event.aggregateType).isEqualTo("ChallengeVideo")
        assertThat(event.aggregateId).isEqualTo(room.id)
        assertThat(event.status).isEqualTo(OutboxStatus.PENDING)

        val payload = objectMapper.readValue(
            event.payload,
            YoutubeVideoPayload::class.java
        )

        assertThat(payload.userId).isEqualTo(user.id)
        assertThat(payload.roomId).isEqualTo(room.id)
        assertThat(payload.youtubeUrl).isEqualTo(youtubeUrl)

        verifyNoInteractions(youtubeMetadataClient)
    }
}
