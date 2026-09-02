package com.examiq.backend.repository;

import com.examiq.backend.entity.Upload;
import com.examiq.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadRepository extends JpaRepository<Upload, Long> {
    List<Upload> findByUploadedBy(User uploadedBy);

    boolean existsByPaper(com.examiq.backend.entity.Paper paper);

    java.util.Optional<Upload> findByPaper(com.examiq.backend.entity.Paper paper);
}
