package com.examiq.backend.repository;

import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Question;
import com.examiq.backend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByPaper(Paper paper);

    List<Question> findByPaper_SubjectAndPaper_StatusOrderByCreatedAtDesc(Subject subject, String status);
}
