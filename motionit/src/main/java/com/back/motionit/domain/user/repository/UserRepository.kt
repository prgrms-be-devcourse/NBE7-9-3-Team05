package com.back.motionit.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.motionit.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	Optional<User> findByKakaoId(Long kakaoId);

	Optional<User> findByNickname(String nickname);

	Optional<User> findByRefreshToken(String refreshToken);

	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);
}
