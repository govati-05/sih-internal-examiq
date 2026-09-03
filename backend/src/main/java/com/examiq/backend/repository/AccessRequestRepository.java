package com.examiq.backend.repository;

import com.examiq.backend.entity.AccessRequest;
import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    List<AccessRequest> findByPaperOrderByCreatedAtDesc(Paper paper);

    List<AccessRequest> findByRequesterOrderByCreatedAtDesc(User requester);

    List<AccessRequest> findByPaper_UploaderOrderByCreatedAtDesc(User uploader);

    Optional<AccessRequest> findTopByPaperAndRequesterOrderByCreatedAtDesc(Paper paper, User requester);

    List<AccessRequest> findByPaperAndRequesterAndStatus(Paper paper, User requester, String status);
}
