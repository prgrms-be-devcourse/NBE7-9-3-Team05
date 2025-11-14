package com.back.motionit.domain.user.controller

import com.back.motionit.domain.user.dto.UpdateUserProfileRequest
import com.back.motionit.domain.user.dto.UserProfileResponse
import com.back.motionit.domain.user.service.UserService
import com.back.motionit.global.request.RequestContext
import com.back.motionit.global.respoonsedata.ResponseData
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
    private val requestContext: RequestContext
) {

    @GetMapping("/profile")
    fun getUserProfile(): ResponseData<UserProfileResponse> {
        val userId = requestContext.actor.id
        val profile = userService.getUserProfile(userId!!)
        return ResponseData.success(profile)
    }

    @PutMapping("/profile")
    fun updateProfile(
        @Valid @RequestBody request: UpdateUserProfileRequest
    ): ResponseData<UserProfileResponse> {
        val userId = requestContext.actor.id
        val response = userService.updateUserProfile(userId!!, request)
        return ResponseData.success(response)
    }
}
