package com.evoting.evotingsystem.repository.projection;

public interface CandidateVoteAggregateView {

    Long getCandidateId();

    String getFullName();

    String getPartyName();

    Long getElectionId();

    String getElectionTitle();
    
    String getImagePath();

    long getVoteCount();
}
