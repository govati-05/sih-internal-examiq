package com.examiq.backend.repository;

import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Upload;
import com.examiq.backend.entity.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {
    List<VerificationLog> findByPaperOrderByCreatedAtDesc(Paper paper);
    List<VerificationLog> findByUploadOrderByCreatedAtDesc(Upload upload);
}
