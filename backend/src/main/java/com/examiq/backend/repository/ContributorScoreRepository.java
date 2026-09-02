package com.examiq.backend.repository;

import com.examiq.backend.entity.ContributorScore;
import com.examiq.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContributorScoreRepository extends JpaRepository<ContributorScore, Long> {
    Optional<ContributorScore> findByUser(User user);
    boolean existsByUser(User user);
}
