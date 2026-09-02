package com.examiq.backend.repository;

import com.examiq.backend.entity.Bookmark;
import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserOrderByCreatedAtDesc(User user);

    Optional<Bookmark> findByUserAndPaper(User user, Paper paper);

    boolean existsByUserAndPaper(User user, Paper paper);
}
