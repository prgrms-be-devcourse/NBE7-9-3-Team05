package com.back.motionit.helper;

import org.springframework.stereotype.Component;

import com.back.motionit.domain.user.entity.User;
import com.back.motionit.domain.user.repository.UserRepository;
import com.back.motionit.factory.UserFactory;

@Component
public class UserHelper {

	private final UserRepository userRepository;

	UserHelper(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User createUser() {
		return userRepository.save(UserFactory.fakeUser());
	}

	public void clearUser() {
		userRepository.deleteAll();
	}
}
