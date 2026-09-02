package com.examiq.backend.repository;

import com.examiq.backend.entity.SubjectAlias;
import com.examiq.backend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectAliasRepository extends JpaRepository<SubjectAlias, Long> {
    Optional<SubjectAlias> findByAlias(String alias);

    Optional<SubjectAlias> findByAliasIgnoreCase(String alias);

    List<SubjectAlias> findBySubject(Subject subject);
}
