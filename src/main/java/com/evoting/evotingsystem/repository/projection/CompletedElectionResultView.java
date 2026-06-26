package com.evoting.evotingsystem.repository.projection;

import java.time.LocalDateTime;

public interface CompletedElectionResultView {

    Long getElectionId();

    String getElectionTitle();

    LocalDateTime getStartsAt();

    LocalDateTime getEndsAt();

    LocalDateTime getPublishedAt();

    long getTotalVotes();
}
