package com.back.motionit.domain.user.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.motionit.domain.user.dto.UpdateUserProfileRequest;
import com.back.motionit.domain.user.dto.UserProfileResponse;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.global.error.code.AuthErrorCode;
import com.back.motionit.global.error.exception.BusinessException;
import com.back.motionit.global.service.AwsCdnSignService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final ObjectProvider<AwsCdnSignService> cdnProvider;

	public UserProfileResponse getUserProfile(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

		String signedUrl = generateProfileUrl(user.getUserProfile());

		return UserProfileResponse.builder()
			.userId(user.getId())
			.email(user.getEmail())
			.nickname(user.getNickname())
			.userProfileUrl(signedUrl)
			.loginType(user.getLoginType())
			.build();
	}

	@Transactional
	public UserProfileResponse updateUserProfile(Long userId, UpdateUserProfileRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

		String newNickname = request.getNickname() != null ? request.getNickname() : user.getNickname();
		String newUserProfile = request.getUserProfile() != null ? request.getUserProfile() : user.getUserProfile();

		if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
			if (userRepository.existsByNickname(request.getNickname())) {
				throw new BusinessException(AuthErrorCode.NICKNAME_DUPLICATED);
			}
		}

		user.update(newNickname, newUserProfile);

		String signedUrl = generateProfileUrl(user.getUserProfile());

		return UserProfileResponse.builder()
			.userId(user.getId())
			.email(user.getEmail())
			.nickname(user.getNickname())
			.userProfileUrl(signedUrl)
			.loginType(user.getLoginType())
			.build();
	}

	private String generateProfileUrl(String userProfile) {
		if (userProfile == null) {
			return null;
		}

		// 외부 URL(http:// 또는 https://로 시작)인 경우 그대로 반환
		if (userProfile.startsWith("http://") || userProfile.startsWith("https://")) {
			return userProfile;
		}

		AwsCdnSignService cdnSignService = cdnProvider.getIfAvailable();

		// S3 ObjectKey인 경우 CDN Sign 적용
		return (cdnSignService != null)
			? cdnSignService.sign(userProfile)
			: "";
	}
}
