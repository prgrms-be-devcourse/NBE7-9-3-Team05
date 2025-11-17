package com.back.motionit.domain.challenge.video;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class YoutubeKeyTest {

	@Value("${youtube.api.key}")
	private String youtubeApiKey;

	@Test
	void checkKeyLoaded() {
		System.out.println("Loaded key: " + youtubeApiKey);
		assertThat(youtubeApiKey).isNotBlank();
	}
}
