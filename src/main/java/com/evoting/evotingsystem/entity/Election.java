package com.evoting.evotingsystem.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "elections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"candidates", "votes"})
public class Election {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Size(min = 5, max = 150)
    @Column(nullable = false, unique = true, length = 150)
    private String title;

    @NotBlank
    @Size(min = 10, max = 1000)
    @Column(nullable = false, length = 1000)
    private String description;

    @Size(max = 120)
    @Column(length = 120)
    private String organizer;

    @Size(max = 1500)
    @Column(length = 1500)
    private String instructions;

    @Size(max = 1500)
    @Column(length = 1500)
    private String timeline;

    @NotNull
    @Future
    @Column(nullable = false)
    private LocalDateTime startsAt;

    @NotNull
    @Future
    @Column(nullable = false)
    private LocalDateTime endsAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ElectionStatus status = ElectionStatus.DRAFT;

    @OneToMany(mappedBy = "election", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Candidate> candidates = new HashSet<>();

    @OneToMany(mappedBy = "election", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Vote> votes = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ElectionStatus getCalculatedStatus() {
        if (status == ElectionStatus.DRAFT || status == ElectionStatus.CANCELLED) {
            return status;
        }

        // Minimum 2 candidates required for an election to move out of DRAFT
        if (candidates == null || candidates.size() < 2) {
            return ElectionStatus.DRAFT;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        if (now.isBefore(startsAt)) {
            return ElectionStatus.UPCOMING;
        } else if (now.isAfter(endsAt)) {
            return ElectionStatus.COMPLETED;
        } else {
            return ElectionStatus.ACTIVE;
        }
    }
}
