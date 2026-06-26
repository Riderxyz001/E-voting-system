package com.evoting.evotingsystem.repository.projection;

import java.time.LocalDateTime;

public interface VoterElectionHistoryView {

    Long getElectionId();

    String getElectionTitle();

    LocalDateTime getElectionStartsAt();

    LocalDateTime getElectionEndsAt();

    long getTotalCandidates();

    Long getSelectedCandidateId();

    String getSelectedCandidateName();

    String getSelectedCandidateParty();

    LocalDateTime getVotedAt();
}
