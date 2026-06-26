package com.evoting.evotingsystem.repository.projection;

import java.time.LocalDateTime;

public interface AdminVoteRowView {
    Long getVoteId();
    String getVoterName();
    String getElectionTitle();
    String getCandidateName();
    LocalDateTime getVotedAt();
}
