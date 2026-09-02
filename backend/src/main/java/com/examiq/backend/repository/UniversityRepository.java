package com.examiq.backend.repository;

import com.examiq.backend.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UniversityRepository extends JpaRepository<University, Long> {
    Optional<University> findByNameIgnoreCase(String name);
}
