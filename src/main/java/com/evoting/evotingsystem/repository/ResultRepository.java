package com.evoting.evotingsystem.repository;

import com.evoting.evotingsystem.entity.Election;
import com.evoting.evotingsystem.entity.ElectionStatus;
import com.evoting.evotingsystem.repository.projection.CompletedElectionResultView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResultRepository extends JpaRepository<Election, Long> {

    long countByStatus(ElectionStatus status);

    @Query("""
            select
                e.id as electionId,
                e.title as electionTitle,
                e.startsAt as startsAt,
                e.endsAt as endsAt,
                e.updatedAt as publishedAt,
                count(v.id) as totalVotes
            from Election e
            left join Vote v on v.election.id = e.id
            where e.status = :status
            group by e.id, e.title, e.startsAt, e.endsAt, e.updatedAt
            order by e.endsAt desc
            """)
    Page<CompletedElectionResultView> findCompletedElectionResults(
            @Param("status") ElectionStatus status,
            Pageable pageable
    );
}
