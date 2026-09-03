package com.examiq.backend.repository;

import com.examiq.backend.entity.QuizAttempt;
import com.examiq.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUserOrderByCreatedAtDesc(User user);

    List<QuizAttempt> findByUserAndStatusOrderByCreatedAtDesc(User user, String status);
}
