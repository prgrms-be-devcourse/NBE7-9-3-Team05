package com.back.motionit.domain.challenge.video.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.video.entity.ChallengeVideo;

public interface ChallengeVideoRepository extends JpaRepository<ChallengeVideo, Long> {

	boolean existsByChallengeRoomAndYoutubeVideoId(ChallengeRoom challengeRoom, String videoId);

	List<ChallengeVideo> findByUserIdAndUploadDate(Long userId, LocalDate today);

	Optional<ChallengeVideo> findByIdAndUserId(Long videoId, Long userId);

	Collection<ChallengeVideo> findByChallengeRoomId(Long roomId);

	boolean existsByChallengeRoomIdAndUploadDate(Long roomId, LocalDate today);
}
