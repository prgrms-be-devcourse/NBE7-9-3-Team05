package com.back.motionit.domain.challenge.room.service

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.participant.service.ChallengeParticipantService
import com.back.motionit.domain.challenge.room.dto.*
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomSummaryRepository
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import com.back.motionit.domain.challenge.video.entity.OpenStatus
import com.back.motionit.domain.challenge.video.service.ChallengeVideoService
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.enums.ChallengeStatus
import com.back.motionit.global.enums.EventEnums
import com.back.motionit.global.error.code.ChallengeRoomErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.global.event.EventPublisher
import com.back.motionit.global.service.AwsS3Service
import org.springframework.beans.factory.ObjectProvider

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class ChallengeRoomService(
    private val challengeRoomRepository: ChallengeRoomRepository,
    private val challengeParticipantService: ChallengeParticipantService,
    private val eventPublisher: EventPublisher,
    private val userRepository: UserRepository,
    private val participantRepository: ChallengeParticipantRepository,
    private val participantService: ChallengeParticipantService,
    private val summaryRepository: ChallengeRoomSummaryRepository,
    private val s3Provider: ObjectProvider<AwsS3Service>,
    private val videoService: ChallengeVideoService,
) {

    private fun s3(): AwsS3Service? = s3Provider.getIfAvailable()

    @Transactional
    fun createRoom(input: CreateRoomRequest, user: User?): CreateRoomResponse {
        if (user == null) {
            throw BusinessException(ChallengeRoomErrorCode.NOT_FOUND_USER)
        }

        val userId = user.id!!

        val host = userRepository.findById(userId)
            .orElseThrow { BusinessException(ChallengeRoomErrorCode.NOT_FOUND_USER) }

        val s3 = s3()

        val imageFileName: String = input.imageFileName
        val contentType: String = input.contentType

        val objectKey = if (s3 != null && imageFileName.isNotBlank()) {
            s3.buildObjectKey(imageFileName)
        } else {
            ""
        }

        val room = mapToRoomObject(input, host, objectKey)
        val createdRoom = challengeRoomRepository.save(room)

        // 방장 자동 참가 처리, 여기서 실패시 방 생성도 롤백 처리됨
        autoJoinAsHost(createdRoom)

        val uploadUrl = if (s3 != null && objectKey.isNotBlank()) {
            s3.createUploadUrl(objectKey, contentType)
        } else {
            ""
        }

        if (input.videoUrl.isNotBlank()) {
            videoService.requestUploadChallengeVideo(
                actorId = host.id!!,
                roomId = createdRoom.id!!,
                youtubeUrl = input.videoUrl,
            )
        }

        return mapToCreateRoomResponse(createdRoom, uploadUrl)
    }

    @Transactional(readOnly = true)
    fun getRooms(user: User?, page: Int, size: Int): GetRoomsResponse {
        val pageable: Pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createDate")
        )

        val pageResult: Page<ChallengeRoom> = summaryRepository.fetchOpenRooms(pageable)
        val rooms: List<ChallengeRoom> = pageResult.content

        if (rooms.isEmpty()) {
            return GetRoomsResponse(countOpenRooms(), listOf())
        }

        val roomIds: List<Long> = rooms.mapNotNull { it.id }
        val joiningSet: Set<Long> =
            if ((user == null)) {
                emptySet()
            } else {
                participantRepository
                    .findJoiningRoomIdsByUserAndRoomIds(user.id!!, roomIds)
                    .toSet()
            }

        val raw = participantRepository.countActiveParticipantsByRoomIds(roomIds)
        val countMap: MutableMap<Long, Int> = HashMap(raw.size)

        for (row in raw) {
            val roomId = row[0] as Long
            val count = row[1] as Long
            countMap[roomId] = count.toInt()
        }

        val summaries = rooms.map {  room: ChallengeRoom ->
            val dDay = ChronoUnit.DAYS.between(LocalDate.now(), room.challengeEndDate.toLocalDate())

            GetRoomSummary(
                id = room.id,
                title = room.title,
                description = room.description,
                capacity = room.capacity,
                dDay = dDay.toInt(),
                roomImage = room.roomImage,
                status = if (user != null && room.id != null && joiningSet.contains(room.id))
                    ChallengeStatus.JOINING
                else
                    ChallengeStatus.JOINABLE,
                current = countMap[room.id] ?: 0
            )
        }

        return GetRoomsResponse(pageResult.totalElements.toInt(), summaries)
    }

    @Transactional
    fun getRoom(roomId: Long): GetRoomResponse {
        val room = challengeRoomRepository.findDetailById(roomId)
            ?: throw BusinessException(ChallengeRoomErrorCode.NOT_FOUND_ROOM)

        return mapToGetRoomResponse(room)
    }

    @Transactional
    fun deleteRoom(roomId: Long, user: User) {
        if (!participantService.checkParticipantIsRoomHost(user.id!!, roomId)) {
            throw BusinessException(ChallengeRoomErrorCode.INVALID_AUTH_USER)
        }

        val deleted = challengeRoomRepository.softDeleteById(roomId)

        if (deleted == 0) {
            throw BusinessException(ChallengeRoomErrorCode.FAILED_DELETE_ROOM)
        }

        eventPublisher.publishEvent(RoomEventDto(EventEnums.ROOM))
    }

    fun mapToRoomObject(input: CreateRoomRequest, user: User, objectKey: String): ChallengeRoom {
        val now = LocalDateTime.now()
        val durationDays = input.duration

        val start = now
        val end = start.plusDays(durationDays.toLong())

        return ChallengeRoom(
            user,
            input.title,
            input.description,
            input.capacity,
            OpenStatus.OPEN,
            start,
            end,
            objectKey,
            null,
        )
    }

    private fun mapToCreateRoomResponse(room: ChallengeRoom, uploadUrl: String): CreateRoomResponse {
        return CreateRoomResponse(
            room.id,
            room.title,
            room.description,
            room.capacity,
            room.openStatus,
            room.challengeStartDate,
            room.challengeEndDate,
            room.roomImage,
            room.getChallengeVideoList(),
            uploadUrl
        )
    }

    private fun mapToGetRoomResponse(room: ChallengeRoom): GetRoomResponse {
        val videos: List<ChallengeVideoDto> = room
            .getChallengeVideoList()
            .map { video: ChallengeVideo -> ChallengeVideoDto(video) }

        val participants = participantRepository
            .findAllByRoomIdWithUser(room.id!!)
            .map { participant: ChallengeParticipant -> ChallengeParticipantDto(participant) }

        return GetRoomResponse(
            room,
            videos,
            participants
        )
    }

    private fun autoJoinAsHost(createdRoom: ChallengeRoom) {
        challengeParticipantService.joinChallengeRoom(
            createdRoom.user.id!!,
            createdRoom.id!!,
            ChallengeParticipantRole.HOST
        )
    }

    fun countOpenRooms(): Int {
        return summaryRepository.countOpenRooms()
    }
}
