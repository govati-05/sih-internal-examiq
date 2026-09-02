package com.examiq.backend.repository;

import com.examiq.backend.entity.AdminAction;
import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminActionRepository extends JpaRepository<AdminAction, Long> {
    List<AdminAction> findByAdminOrderByCreatedAtDesc(User admin);
    List<AdminAction> findByPaperOrderByCreatedAtDesc(Paper paper);
}
