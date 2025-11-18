package com.back.motionit.global.init

import com.back.motionit.domain.challenge.comment.entity.Comment
import com.back.motionit.domain.challenge.comment.repository.CommentRepository
import com.back.motionit.domain.challenge.mission.entity.ChallengeMissionStatus
import com.back.motionit.domain.challenge.mission.repository.ChallengeMissionStatusRepository
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant
import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipantRole
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo
import com.back.motionit.domain.challenge.video.entity.OpenStatus
import com.back.motionit.domain.challenge.video.external.youtube.dto.YoutubeVideoMetadata
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository
import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ThreadLocalRandom

@Configuration
@Profile("perf")
class PerfDataInitializer(
    private val userRepository: UserRepository,
    private val challengeRoomRepository: ChallengeRoomRepository,
    private val challengeParticipantRepository: ChallengeParticipantRepository,
    private val challengeVideoRepository: ChallengeVideoRepository,
    private val challengeMissionStatusRepository: ChallengeMissionStatusRepository,
    private val commentRepository: CommentRepository,
) {

    private val log = KotlinLogging.logger {}

    @Bean
    fun initPerfDummyData(): ApplicationRunner = ApplicationRunner {

        if (challengeRoomRepository.count() > 0) {
            log.info { "✅ perf 데이터 이미 존재. 초기화 스킵" }
            return@ApplicationRunner
        }

        log.info { "🚀 perf 프로파일용 더미 데이터 생성 시작" }

        /* ----------------------------------
         * 1️⃣ host 생성
         * ---------------------------------- */
        val host = userRepository.save(
            User.builder()
                .kakaoId(9001L)
                .email("perf_host@example.com")
                .nickname("PerfHost")
                .password("1234")
                .loginType(LoginType.KAKAO)
                .userProfile("https://picsum.photos/100?perf1")
                .build()
        )

        /* ----------------------------------
         * 2️⃣ 일반 유저 200명 생성
         * ---------------------------------- */
        val users = (1..200).map { i ->
            userRepository.save(
                User.builder()
                    .kakaoId(9100L + i)
                    .email("perf_user$i@example.com")
                    .nickname("PerfUser$i")
                    .password("pass$i")
                    .loginType(LoginType.KAKAO)
                    .userProfile("https://picsum.photos/100?perf${i + 1}")
                    .build()
            )
        }

        /* ----------------------------------
         * 3️⃣ 방 15개 생성 (host가 전부 소유)
         * ---------------------------------- */
        val rooms = (1..15).map { idx ->
            challengeRoomRepository.save(
                ChallengeRoom(
                    user = host,
                    title = "🔥 K6 부하테스트 전용 방 #$idx",
                    description = "부하테스트용 방입니다. ($idx)",
                    capacity = 100,
                    openStatus = OpenStatus.OPEN,
                    challengeStartDate = LocalDateTime.now().minusDays(1),
                    challengeEndDate = LocalDateTime.now().plusDays(7),
                    roomImage = "images/test/perf_room_$idx.png",
                    deletedAt = null
                )
            )
        }

        /* ----------------------------------
         * 4️⃣ 각 방에 host 참가자 추가
         * ---------------------------------- */
        val roomParticipantsMap = mutableMapOf<Long, MutableList<ChallengeParticipant>>()

        rooms.forEach { room ->
            val hostParticipant = challengeParticipantRepository.save(
                ChallengeParticipant.create(
                    user = host,
                    room = room,
                    role = ChallengeParticipantRole.HOST,
                    quited = false,
                    challengeStatus = false,
                    quitDate = null
                )
            )
            roomParticipantsMap[room.id!!] = mutableListOf(hostParticipant)
        }

        /* ----------------------------------
         * 5️⃣ 유저 → 방 매핑 (고정 규칙: (userId-1)%15)
         * ---------------------------------- */
        val participants = users.map { user ->
            val roomIndex = ((user.id!!.toInt() - 1) % rooms.size)
            val assignedRoom = rooms[roomIndex]

            val participant = ChallengeParticipant.create(
                user = user,
                room = assignedRoom,
                role = ChallengeParticipantRole.NORMAL,
                quited = false,
                challengeStatus = false,
                quitDate = null
            )

            roomParticipantsMap.getOrPut(assignedRoom.id!!) { mutableListOf() }.add(participant)
            participant
        }

        challengeParticipantRepository.saveAll(participants)

        /* ----------------------------------
         * 기존방 이후 join 부하테스트용 방 200개 생성
         * ---------------------------------- */
        (1..200).map { i ->
            challengeRoomRepository.save(
                ChallengeRoom(
                    user = host,
                    title = "Join 부하테스트 전용 방 #${15 + i}",
                    description = "성능 측정을 위한 빈 챌린지 방입니다. (join 테스트용 #${15 + i})",
                    capacity = 100,
                    openStatus = OpenStatus.OPEN,
                    challengeStartDate = LocalDateTime.now().minusDays(1),
                    challengeEndDate = LocalDateTime.now().plusDays(7),
                    roomImage = "images/test/perf_room_extra_${15 + i}.png",
                    deletedAt = null
                )
            )
        }

        /* ----------------------------------
         * 6️⃣ 오늘의 영상 1개씩 생성
         * ---------------------------------- */
        val todayVideos = rooms.map { r ->
            val metadata = YoutubeVideoMetadata(
                videoId = "2fpek3wzSZo",
                title = "오늘의 퍼포먼스 테스트 영상 - Room ${r.id}",
                thumbnailUrl = "https://i.ytimg.com/vi/2fpek3wzSZo/hqdefault.jpg",
                durationSeconds = 3528
            )

            val video = ChallengeVideo.of(
                room = r,
                user = host,
                metadata = metadata,
                isTodayMission = true
            )

            challengeVideoRepository.save(video)
        }

        /* ----------------------------------
         * 7️⃣ 오늘 미션 상태 생성
         * ---------------------------------- */
        val missions = roomParticipantsMap.values.flatten().map { participant ->
            ChallengeMissionStatus.create(participant, LocalDate.now())
        }

        challengeMissionStatusRepository.saveAll(missions)

        /* ----------------------------------
         * 8️⃣ 댓글 50개 생성
         * ---------------------------------- */
        val rnd = ThreadLocalRandom.current()
        val seedComments = rooms.flatMap { r ->
            val plist = roomParticipantsMap[r.id] ?: emptyList()
            if (plist.isEmpty()) return@flatMap emptyList<Comment>()

            (1..50).map { idx ->
                val writer = plist[rnd.nextInt(plist.size)]
                val author = writer.user

                Comment(
                    deletedAt = null,
                    challengeRoom = r,
                    user = author,
                    content = "Perf seed comment #$idx in room ${r.id} by ${author.nickname}",
                    likeCount = 0,
                    version = 0L   // Optimistic Lock 초기값
                )
            }
        }

        commentRepository.saveAll(seedComments)

        log.info {
            "🎯 perf 더미데이터 생성 완료! rooms=${rooms.size}, users=${users.size}, comments=${seedComments.size}, videos=${todayVideos.size}"
        }
    }
}