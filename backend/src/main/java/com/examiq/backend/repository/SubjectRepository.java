package com.examiq.backend.repository;

import com.examiq.backend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByCanonicalName(String canonicalName);

    Optional<Subject> findByCanonicalNameIgnoreCase(String canonicalName);

    Optional<Subject> findByName(String name);

    Optional<Subject> findByNameIgnoreCase(String name);
}
