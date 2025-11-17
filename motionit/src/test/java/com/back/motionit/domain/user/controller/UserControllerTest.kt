package com.back.motionit.domain.user.controller

import com.back.motionit.domain.user.dto.UpdateUserProfileRequest
import com.back.motionit.domain.user.dto.UserProfileResponse
import com.back.motionit.domain.user.entity.LoginType
import com.back.motionit.domain.user.entity.User
import com.back.motionit.helper.UserHelper
import com.back.motionit.security.SecurityUser
import com.back.motionit.support.BaseIntegrationTest
import com.back.motionit.support.SecuredIntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SecuredIntegrationTest
class UserControllerTest : BaseIntegrationTest() {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var userHelper: UserHelper

    @Autowired
    lateinit var mapper: ObjectMapper

    private lateinit var user: User
    private lateinit var securityUser: SecurityUser
    private lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setUp() {
        user = userHelper.createUser()

        val authorities = AuthorityUtils.createAuthorityList("ROLE")
        securityUser = SecurityUser(user.id!!, user.password!!, user.nickname, authorities)
        authentication = UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Nested
    @DisplayName("GET /api/v1/users/profile - 사용자 프로필 조회")
    internal inner class GetUserProfileTest {
        private val profileUrl = "/api/v1/users/profile"

        @Test
        @DisplayName("성공 - 사용자 프로필을 정상적으로 조회")
        fun getUserProfile_success() {
            mvc.perform(
                MockMvcRequestBuilders.get(profileUrl)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(user.id!!))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(user.email))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(user.nickname))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.loginType").value(user.loginType.toString()))
        }

        @Test
        @DisplayName("실패 - 인증되지 않은 사용자")
        fun getUserProfile_unauthorized() {
            SecurityContextHolder.clearContext()

            mvc.perform(
                MockMvcRequestBuilders.get(profileUrl)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/profile - 사용자 프로필 수정")
    internal inner class UpdateUserProfileTest {
        private val profileUrl = "/api/v1/users/profile"

        @Test
        @DisplayName("성공 - 닉네임 수정")
        fun updateProfile_nickname_success() {
            val request = UpdateUserProfileRequest(
                nickname = "newNickname",
                userProfile = null
            )

            mvc.perform(
                MockMvcRequestBuilders.put(profileUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(user.id!!))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value("newNickname"))
        }

        @Test
        @DisplayName("성공 - 프로필 이미지 수정")
        fun updateProfile_image_success() {
            val request = UpdateUserProfileRequest(
                nickname = null,
                userProfile = "profiles/new-image.png"
            )

            mvc.perform(
                MockMvcRequestBuilders.put(profileUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(user.id!!))
        }

        @Test
        @DisplayName("성공 - 닉네임과 프로필 이미지 모두 수정")
        fun updateProfile_both_success() {
            val request = UpdateUserProfileRequest(
                nickname = "updatedNickname",
                userProfile = "profiles/updated-image.png"
            )

            mvc.perform(
                MockMvcRequestBuilders.put(profileUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(user.id!!))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value("updatedNickname"))
        }

        @Test
        @DisplayName("실패 - 닉네임이 너무 짧음 (1자)")
        fun updateProfile_nickname_tooShort() {
            val request = UpdateUserProfileRequest(
                nickname = "a",
                userProfile = null
            )

            mvc.perform(
                MockMvcRequestBuilders.put(profileUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
        }

        @Test
        @DisplayName("실패 - 닉네임이 너무 김 (51자)")
        fun updateProfile_nickname_tooLong() {
            val request = UpdateUserProfileRequest(
                nickname = "a".repeat(51),
                userProfile = null
            )

            mvc.perform(
                MockMvcRequestBuilders.put(profileUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
        }

        @Test
        @DisplayName("실패 - 중복된 닉네임")
        fun updateProfile_nickname_duplicate() {
            val anotherUser = userHelper.createUser()

            val request = UpdateUserProfileRequest(
                nickname = anotherUser.nickname,
                userProfile = null
            )

            mvc.perform(
                MockMvcRequestBuilders.put(profileUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().is4xxClientError)
        }

        @Test
        @DisplayName("실패 - 인증되지 않은 사용자")
        fun updateProfile_unauthorized() {
            SecurityContextHolder.clearContext()

            val request = UpdateUserProfileRequest(
                nickname = "newNickname",
                userProfile = null
            )

            mvc.perform(
                MockMvcRequestBuilders.put(profileUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }
    }
}
