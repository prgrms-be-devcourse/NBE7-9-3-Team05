package com.back.motionit.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChallengeParticipantErrorCode implements ErrorCode {
	NOT_FOUND_USER(HttpStatus.NOT_FOUND, "R-500", "유저를 찾을 수 없습니다."),
	CANNOT_FIND_CHALLENGE_ROOM(HttpStatus.NOT_FOUND, "R-501", "챌린지 룸을 찾을 수 없습니다."),
	ALREADY_JOINED(HttpStatus.BAD_REQUEST, "R-502", "이미 해당 챌린지에 참가한 유저입니다."),
	FULL_JOINED_ROOM(HttpStatus.BAD_REQUEST, "R-503", "챌린지 참가 인원이 초과되었습니다."),
	NO_PARTICIPANT_IN_ROOM(HttpStatus.BAD_REQUEST, "R-504", "챌린지에 참가한 유저가 아닙니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
