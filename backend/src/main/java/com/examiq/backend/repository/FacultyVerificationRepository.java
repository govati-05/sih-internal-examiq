package com.examiq.backend.repository;

import com.examiq.backend.entity.FacultyVerification;
import com.examiq.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyVerificationRepository extends JpaRepository<FacultyVerification, Long> {
    Optional<FacultyVerification> findByFaculty(User faculty);

    List<FacultyVerification> findByVerificationStatus(String status);
}
