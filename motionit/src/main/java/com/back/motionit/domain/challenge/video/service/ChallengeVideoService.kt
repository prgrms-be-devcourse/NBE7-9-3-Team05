package com.back.motionit.domain.challenge.video.service

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.challenge.validator.ChallengeAuthValidator
import com.back.motionit.domain.challenge.video.dto.YoutubeVideoPayload
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo.Companion.of
import com.back.motionit.domain.challenge.video.external.youtube.YoutubeMetadataClient
import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.enums.OutboxEventType
import com.back.motionit.global.error.code.ChallengeVideoErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.global.outbox.entity.OutboxEvent
import com.back.motionit.global.outbox.repository.OutboxEventRepository
import com.fasterxml.jackson.databind.ObjectMapper
import lombok.RequiredArgsConstructor
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@RequiredArgsConstructor
class ChallengeVideoService(
    private val challengeVideoRepository: ChallengeVideoRepository,
    private val challengeRoomRepository: ChallengeRoomRepository,
    private val userRepository: UserRepository,
    private val youtubeMetadataClient: YoutubeMetadataClient, // 유튜브 메타데이터 클라이언트
    private val challengeAuthValidator: ChallengeAuthValidator,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun requestUploadChallengeVideo(actorId: Long, roomId: Long, youtubeUrl: String) {
        challengeAuthValidator.validateActiveParticipant(actorId, roomId)

        val payload = YoutubeVideoPayload(
            userId = actorId,
            roomId = roomId,
            youtubeUrl = youtubeUrl.trim(),
        )

        val payloadJson = objectMapper.writeValueAsString(payload)

        val outboxEvent = OutboxEvent(
            eventType = OutboxEventType.YOUTUBE_VIDEO,
            aggregateType = "ChallengeVideo",
            aggregateId = roomId,
            payload = payloadJson,
        )

        outboxEventRepository.save(outboxEvent)
    }

    @Transactional
    fun saveChallengeVideo(
        actorId: Long,
        roomId: Long,
        metadata: YoutubeVideoMetadata,
        isTodayMission: Boolean = true,
    ): ChallengeVideo {
        val user = getUserOrThrow(actorId)
        val participant = challengeAuthValidator.validateActiveParticipantWithRoom(actorId, roomId)
        val challengeRoom = participant.challengeRoom

        validateDuplicateVideo(challengeRoom, metadata.videoId)

        val video = of(challengeRoom, user, metadata, isTodayMission)
        return challengeVideoRepository.save(video)
    }

    // 오늘 업로드된 모든 '오늘의 미션 영상' 조회 (방 전체 기준)
    @Transactional(readOnly = true)
    fun getTodayMissionVideos(actorId: Long, roomId: Long): List<ChallengeVideo> {
        challengeAuthValidator.validateActiveParticipant(actorId, roomId)

        return challengeVideoRepository.findTodayVideos(
            roomId = roomId,
            today = LocalDate.now()
        )
    }

    //사용자가 직접 업로드한 영상 삭제
    @Transactional
    fun deleteVideoByUser(actorId: Long, roomId: Long, videoId: Long) {
        challengeAuthValidator.validateActiveParticipant(actorId, roomId)
        val video: ChallengeVideo =
            challengeVideoRepository.findByIdAndUserId(videoId, actorId)
                ?: throw BusinessException(ChallengeVideoErrorCode.VIDEO_NOT_FOUND_OR_FORBIDDEN)

        challengeVideoRepository.delete(video)
    }

    // 특정 사용자가 오늘 업로드한 영상 목록 조회
    @Transactional(readOnly = true)
    fun getTodayVideosByUser(actorId: Long): List<ChallengeVideo> {
        return challengeVideoRepository.findByUserIdAndUploadDate(actorId, LocalDate.now())
    }

    private fun getUserOrThrow(userId: Long): User {
        return userRepository.findByIdOrNull(userId)
            ?: throw BusinessException(ChallengeVideoErrorCode.NOT_FOUND_USER)
    }

    private fun validateDuplicateVideo(room: ChallengeRoom, videoId: String) {
        if (challengeVideoRepository.existsByChallengeRoomAndYoutubeVideoId(room, videoId)) {
            throw BusinessException(ChallengeVideoErrorCode.DUPLICATE_VIDEO_IN_ROOM)
        }
    }
}
