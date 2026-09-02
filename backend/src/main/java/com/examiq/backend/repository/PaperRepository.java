package com.examiq.backend.repository;

import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Subject;
import com.examiq.backend.entity.University;
import com.examiq.backend.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaperRepository extends JpaRepository<Paper, Long> {
    List<Paper> findByStatusOrderByCreatedAtDesc(String status, Sort sort);

    List<Paper> findByUploaderOrderByCreatedAtDesc(User uploader);

    Optional<Paper> findByTitleAndSubjectAndUniversityAndYearAndExamType(
            String title, Subject subject, University university, Integer year, String examType);

    boolean existsByTitleAndSubjectAndUniversityAndYearAndExamType(
            String title, Subject subject, University university, Integer year, String examType);

    Optional<Paper> findByFileHash(String fileHash);

    boolean existsByFileHash(String fileHash);
}
