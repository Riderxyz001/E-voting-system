package com.evoting.evotingsystem.repository;

import com.evoting.evotingsystem.entity.Vote;
import com.evoting.evotingsystem.repository.projection.AdminVoteRowView;
import com.evoting.evotingsystem.repository.projection.ElectionVoteTotalView;
import com.evoting.evotingsystem.repository.projection.VoterElectionHistoryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    long countByElectionId(Long electionId);

    long countByVoterId(Long voterId);

    boolean existsByVoterIdAndElectionId(Long voterId, Long electionId);

    long countByVoterIdAndElectionIdIn(Long voterId, Collection<Long> electionIds);

    @Query("""
            select
                v.election.id as electionId,
                count(v.id) as totalVotes
            from Vote v
            where v.election.id in :electionIds
            group by v.election.id
            """)
    List<ElectionVoteTotalView> findVoteTotalsForElectionIds(@Param("electionIds") Collection<Long> electionIds);

    @Query("""
            select count(distinct v.election.id)
            from Vote v
            where v.voter.id = :voterId
            """)
    long countDistinctElectionsVotedByVoterId(@Param("voterId") Long voterId);

    @Query("""
            select
                e.id as electionId,
                e.title as electionTitle,
                e.startsAt as electionStartsAt,
                e.endsAt as electionEndsAt,
                (
                    select count(c2.id)
                    from Candidate c2
                    where c2.election.id = e.id
                ) as totalCandidates,
                c.id as selectedCandidateId,
                c.fullName as selectedCandidateName,
                c.partyName as selectedCandidateParty,
                v.createdAt as votedAt
            from Election e
            left join Vote v on v.election.id = e.id and v.voter.id = :voterId
            left join Candidate c on c.id = v.candidate.id
            where e.endsAt >= :now or v.id is not null
            order by e.startsAt desc
            """)
    Page<VoterElectionHistoryView> findMyVoteHistory(@Param("voterId") Long voterId, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
            select
                v.id as voteId,
                v.voter.fullName as voterName,
                v.election.title as electionTitle,
                v.candidate.fullName as candidateName,
                v.createdAt as votedAt
            from Vote v
            where (:electionId is null or v.election.id = :electionId)
            order by v.createdAt desc
            """)
    Page<AdminVoteRowView> findAdminVoteRows(@Param("electionId") Long electionId, Pageable pageable);
}
