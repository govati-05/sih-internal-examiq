package com.examiq.backend.repository;

import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByPaper(Paper paper);
}
