package com.back.motionit.helper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.challenge.room.repository.ChallengeRoomRepository;
import com.back.motionit.domain.challenge.video.repository.ChallengeVideoRepository;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.factory.ChallengeRoomFactory;

@Component
public class ChallengeRoomHelper {

	private ChallengeRoomRepository challengeRoomRepository;
	private ChallengeVideoRepository challengeVideoRepository;

	ChallengeRoomHelper(
		ChallengeRoomRepository challengeRoomRepository,
		ChallengeVideoRepository challengeVideoRepository
	) {
		this.challengeRoomRepository = challengeRoomRepository;
		this.challengeVideoRepository = challengeVideoRepository;
	}

	public ChallengeRoom createChallengeRoom(User user) {
		return challengeRoomRepository.save(ChallengeRoomFactory.fakeChallengeRoom(user));
	}

	public static MockMultipartHttpServletRequestBuilder createRoomRequest(
		String url, Map<String, String> params, MockMultipartFile image
	) {
		return handleRoomRequest(url, params, image, "POST");
	}

	public static MockMultipartHttpServletRequestBuilder handleRoomRequest(
		String url, Map<String, String> params, MockMultipartFile image, String method
	) {
		MockMultipartHttpServletRequestBuilder builder = multipart(url);

		if (image != null) {
			builder.file(image);
		}

		params.forEach(builder::param);
		builder.contentType(MediaType.MULTIPART_FORM_DATA);

		// 기본적으로 multipart는 POST 요청이므로, method가 다른 경우 요청 메서드 변경
		if (!"POST".equalsIgnoreCase(method.toUpperCase())) {
			builder.with(req -> {
				req.setMethod(method.toUpperCase());
				return req;
			});
		}

		return builder;
	}

	public void clearChallengeRoom() {
		challengeVideoRepository.deleteAll();
		challengeRoomRepository.deleteAll();
	}
}
