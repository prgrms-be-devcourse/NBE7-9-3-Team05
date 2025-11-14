package com.back.motionit.helper

import com.back.motionit.domain.user.entity.User
import com.back.motionit.domain.user.repository.UserRepository
import com.back.motionit.factory.UserFactory.fakeUser
import org.springframework.stereotype.Component

@Component
class UserHelper internal constructor(
    private val userRepository: UserRepository
) {
    fun createUser(): User = userRepository.save(fakeUser())
}
