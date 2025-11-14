package com.back.motionit.domain.user.service

import com.back.motionit.domain.user.dto.UpdateUserProfileRequest
import com.back.motionit.domain.user.dto.UserProfileResponse
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.global.error.code.AuthErrorCode
import com.back.motionit.global.error.exception.BusinessException
import com.back.motionit.global.service.AwsCdnSignService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val cdnProvider: ObjectProvider<AwsCdnSignService>
) {

    fun getUserProfile(userId: Long): UserProfileResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(AuthErrorCode.USER_NOT_FOUND) }

        val signedUrl = generateProfileUrl(user.userProfile)

        return UserProfileResponse.builder()
            .userId(user.id!!)
            .email(user.email)
            .nickname(user.nickname)
            .userProfileUrl(signedUrl)
            .loginType(user.loginType)
            .build()
    }

    @Transactional
    fun updateUserProfile(userId: Long, request: UpdateUserProfileRequest): UserProfileResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(AuthErrorCode.USER_NOT_FOUND) }

        val newNickname = request.nickname ?: user.nickname
        val newUserProfile = request.userProfile ?: user.userProfile

        if (request.nickname != null && request.nickname != user.nickname) {
            if (userRepository.existsByNickname(request.nickname)) {
                throw BusinessException(AuthErrorCode.NICKNAME_DUPLICATED)
            }
        }

        user.update(newNickname, newUserProfile)

        val signedUrl = generateProfileUrl(user.userProfile)

        return UserProfileResponse.builder()
            .userId(user.id!!)
            .email(user.email)
            .nickname(user.nickname)
            .userProfileUrl(signedUrl)
            .loginType(user.loginType)
            .build()
    }

    private fun generateProfileUrl(userProfile: String?): String? {
        if (userProfile == null) {
            return null
        }

        // 외부 URL(http:// 또는 https://로 시작)인 경우 그대로 반환
        if (userProfile.startsWith("http://") || userProfile.startsWith("https://")) {
            return userProfile
        }

        val cdnSignService = cdnProvider.getIfAvailable()

        // S3 ObjectKey인 경우 CDN Sign 적용
        return cdnSignService?.sign(userProfile) ?: ""
    }
}
