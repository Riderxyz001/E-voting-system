package com.evoting.evotingsystem.repository;

import com.evoting.evotingsystem.entity.Election;
import com.evoting.evotingsystem.entity.ElectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ElectionRepository extends JpaRepository<Election, Long> {

    @Query("""
            select count(e.id)
            from Election e
            where e.status in :statuses and e.endsAt > :now
            """)
    long countActiveElections(@Param("statuses") Collection<ElectionStatus> statuses, @Param("now") LocalDateTime now);

    List<Election> findTop6ByStatusInAndEndsAtAfterOrderByStartsAtAsc(Collection<ElectionStatus> statuses, LocalDateTime now);

    List<Election> findTop5ByStatusOrderByEndsAtDesc(ElectionStatus status);

    @Query("""
            select e.id
            from Election e
            where e.status in :statuses and e.endsAt > :now
            """)
    List<Long> findActiveElectionIds(@Param("statuses") Collection<ElectionStatus> statuses, @Param("now") LocalDateTime now);

    @Query("""
            select count(e.id)
            from Election e
            where e.startsAt > :now
              and e.status in :statuses
              and e.id not in (
                    select v.election.id
                    from Vote v
                    where v.voter.id = :voterId
              )
            """)
    long countUpcomingAvailableForVoter(
            @Param("voterId") Long voterId,
            @Param("statuses") Collection<ElectionStatus> statuses,
            @Param("now") LocalDateTime now
    );

    Page<Election> findByStatusIn(Collection<ElectionStatus> statuses, Pageable pageable);

    Page<Election> findByTitleContainingIgnoreCaseAndStatusIn(String title, Collection<ElectionStatus> statuses, Pageable pageable);

    long countByStatusIn(Collection<ElectionStatus> statuses);
}
