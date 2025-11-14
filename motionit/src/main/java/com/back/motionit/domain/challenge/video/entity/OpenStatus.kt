package com.back.motionit.domain.challenge.video.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OpenStatus {
	OPEN("open"),
	CLOSED("closed"),
	DELETED("deleted");

	private final String value;
}
