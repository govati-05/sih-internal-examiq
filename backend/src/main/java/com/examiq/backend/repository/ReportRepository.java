package com.examiq.backend.repository;

import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByPaperOrderByCreatedAtDesc(Paper paper);
    List<Report> findByStatusOrderByCreatedAtDesc(String status);
}
