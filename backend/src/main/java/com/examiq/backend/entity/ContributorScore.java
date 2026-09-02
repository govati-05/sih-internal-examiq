package com.examiq.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "contributor_scores")
@Getter
@Setter
@NoArgsConstructor
public class ContributorScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private Integer score = 0;

    @Column(length = 30)
    private String tier = "NEW_CONTRIBUTOR";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void prePersistOrUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
