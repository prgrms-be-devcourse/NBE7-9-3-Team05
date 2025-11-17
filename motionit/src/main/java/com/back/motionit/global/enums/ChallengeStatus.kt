package com.back.motionit.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChallengeStatus {
	ALL("all"),
	JOINING("joining"),
	JOINABLE("joinable");

	public final String value;
}
