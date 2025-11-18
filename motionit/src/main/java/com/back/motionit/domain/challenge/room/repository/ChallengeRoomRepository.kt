package com.back.motionit.domain.challenge.room.repository

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom
import com.back.motionit.domain.challenge.video.entity.OpenStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ChallengeRoomRepository : JpaRepository<ChallengeRoom, Long>, ChallengeRoomRepositoryCustom {
    // PESSIMISTIC_WRITE 락을 사용하여 해당 챌린지룸을 수정할 때 다른 트랜잭션이 접근하지 못하도록 함
    // 유저 참가시에 정원 초과를 방지하기 위해 사용, 단순 조회(목록 등)에는 절대 쓰지 말것
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChallengeRoom c WHERE c.id = :id")
    fun findByIdWithLock(@Param("id") id: Long): ChallengeRoom?

    fun findByOpenStatus(openStatus: OpenStatus, pageable: Pageable): Page<ChallengeRoom>

    @EntityGraph(attributePaths = ["challengeVideoList"])
    fun findWithVideosById(id: Long): ChallengeRoom?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
		UPDATE ChallengeRoom r
			SET r.deletedAt = CURRENT_TIMESTAMP,
				r.openStatus = 'CLOSED'
		WHERE r.id = :id
		
		"""
    )
    fun softDeleteById(@Param("id") id: Long): Int

    @Query(value = "SELECT deleted_at FROM challenge_rooms WHERE id = :id", nativeQuery = true)
    fun findDeletedAtRaw(@Param("id") id: Long): LocalDateTime?
}
