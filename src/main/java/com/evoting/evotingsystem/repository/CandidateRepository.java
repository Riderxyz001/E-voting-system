package com.evoting.evotingsystem.repository;

import com.evoting.evotingsystem.entity.Candidate;
import com.evoting.evotingsystem.entity.ElectionStatus;
import com.evoting.evotingsystem.repository.projection.CandidateVoteAggregateView;
import com.evoting.evotingsystem.repository.projection.ElectionCandidateCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    long countByElectionId(Long electionId);

    List<Candidate> findByElectionIdOrderByFullNameAsc(Long electionId);

    @Query("""
            select
                c.id as candidateId,
                c.fullName as fullName,
                c.partyName as partyName,
                c.imagePath as imagePath,
                e.id as electionId,
                e.title as electionTitle,
                count(v.id) as voteCount
            from Candidate c
            join c.election e
            left join c.votes v
            where e.status in :statuses
            group by c.id, c.fullName, c.partyName, c.imagePath, e.id, e.title
            order by count(v.id) desc, c.fullName asc
            """)
    List<CandidateVoteAggregateView> findTopCandidatesByStatuses(@Param("statuses") Collection<ElectionStatus> statuses, Pageable pageable);

    @Query("""
            select
                c.id as candidateId,
                c.fullName as fullName,
                c.partyName as partyName,
                c.imagePath as imagePath,
                e.id as electionId,
                e.title as electionTitle,
                count(v.id) as voteCount
            from Candidate c
            join c.election e
            left join c.votes v
            where e.id in :electionIds
            group by c.id, c.fullName, c.partyName, c.imagePath, e.id, e.title
            order by e.id asc, count(v.id) desc, c.fullName asc
            """)
    List<CandidateVoteAggregateView> findCandidateVotesForElectionIds(@Param("electionIds") Collection<Long> electionIds);

    @Query("""
            select
                c.election.id as electionId,
                count(c.id) as candidateCount
            from Candidate c
            where c.election.id in :electionIds
            group by c.election.id
            """)
    List<ElectionCandidateCountView> findCandidateCountsByElectionIds(@Param("electionIds") Collection<Long> electionIds);

    Page<Candidate> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
}
