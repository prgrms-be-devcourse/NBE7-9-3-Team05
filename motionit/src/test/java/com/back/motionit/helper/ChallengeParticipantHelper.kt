package com.back.motionit.helper;

import org.springframework.stereotype.Component;

import com.back.motionit.domain.challenge.participant.entity.ChallengeParticipant;
import com.back.motionit.domain.challenge.participant.repository.ChallengeParticipantRepository;
import com.back.motionit.domain.challenge.room.entity.ChallengeRoom;
import com.back.motionit.domain.user.entity.User;
import com.back.motionit.factory.ChallengeParticipantFactory;

@Component
public class ChallengeParticipantHelper {
	private ChallengeParticipantRepository participantRepository;

	public ChallengeParticipantHelper(
		ChallengeParticipantRepository participantRepository
	) {
		this.participantRepository = participantRepository;
	}

	public ChallengeParticipant createHostParticipant(User user, ChallengeRoom room) {
		return participantRepository.save(ChallengeParticipantFactory.fakeHost(user, room));
	}

	public ChallengeParticipant createNormalParticipant(User user, ChallengeRoom room) {
		return participantRepository.save(ChallengeParticipantFactory.fakeParticipant(user, room));
	}
}
