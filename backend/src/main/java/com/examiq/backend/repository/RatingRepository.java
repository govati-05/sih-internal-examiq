package com.examiq.backend.repository;

import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Rating;
import com.examiq.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByPaper(Paper paper);
    Optional<Rating> findByPaperAndUser(Paper paper, User user);
}
