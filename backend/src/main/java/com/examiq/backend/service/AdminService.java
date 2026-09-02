package com.examiq.backend.service;

import com.examiq.backend.entity.*;
import com.examiq.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final AdminActionRepository adminActionRepository;
    private final NotificationService notificationService;
    private final ContributorScoreService contributorScoreService;
    private final QuestionExtractionService questionExtractionService;

    public AdminService(PaperRepository paperRepository, UserRepository userRepository,
                      AdminActionRepository adminActionRepository, NotificationService notificationService,
                      ContributorScoreService contributorScoreService,
                      QuestionExtractionService questionExtractionService) {
        this.paperRepository = paperRepository;
        this.userRepository = userRepository;
        this.adminActionRepository = adminActionRepository;
        this.notificationService = notificationService;
        this.contributorScoreService = contributorScoreService;
        this.questionExtractionService = questionExtractionService;
    }

    @Transactional
    public Paper approvePaper(Long paperId, Long adminId, String reason) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        paper.setStatus("APPROVED");
        Paper savedPaper = paperRepository.save(paper);

        // Log admin action
        logAdminAction(admin, paper, "APPROVE", reason);

        // Notify uploader
        notificationService.createNotification(
                paper.getUploader().getId(),
                "Paper Approved",
                "Your paper '" + paper.getTitle() + "' has been approved.",
                "PAPER_APPROVED"
        );

        // Award points to contributor
        contributorScoreService.addPoints(paper.getUploader().getId(), 5);

        questionExtractionService.extractAndStoreQuestions(savedPaper);

        return savedPaper;
    }

    @Transactional
    public Paper rejectPaper(Long paperId, Long adminId, String reason) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        paper.setStatus("REJECTED");
        Paper savedPaper = paperRepository.save(paper);

        // Log admin action
        logAdminAction(admin, paper, "REJECT", reason);

        // Notify uploader
        notificationService.createNotification(
                paper.getUploader().getId(),
                "Paper Rejected",
                "Your paper '" + paper.getTitle() + "' has been rejected. Reason: " + reason,
                "PAPER_REJECTED"
        );

        // Deduct points from contributor
        contributorScoreService.subtractPoints(paper.getUploader().getId(), 2);

        return savedPaper;
    }

    @Transactional
    public Paper requestReupload(Long paperId, Long adminId, String reason) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        paper.setStatus("FLAGSED");
        Paper savedPaper = paperRepository.save(paper);

        // Log admin action
        logAdminAction(admin, paper, "REQUEST_REUPLOAD", reason);

        // Notify uploader
        notificationService.createNotification(
                paper.getUploader().getId(),
                "Re-upload Requested",
                "Your paper '" + paper.getTitle() + "' requires re-upload. Reason: " + reason,
                "REUPLOAD_REQUESTED"
        );

        return savedPaper;
    }

    @Transactional
    public User suspendUser(Long userId, Long adminId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        user.setStatus("SUSPENDED");
        User savedUser = userRepository.save(user);

        // Log admin action
        AdminAction action = new AdminAction();
        action.setAdmin(admin);
        action.setUser(user);
        action.setActionType("SUSPEND");
        action.setReason(reason);
        adminActionRepository.save(action);

        // Notify user
        notificationService.createNotification(
                userId,
                "Account Suspended",
                "Your account has been suspended. Reason: " + reason,
                "ACCOUNT_SUSPENDED"
        );

        return savedUser;
    }

    @Transactional
    public User banUser(Long userId, Long adminId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        user.setStatus("BANNED");
        User savedUser = userRepository.save(user);

        // Log admin action
        AdminAction action = new AdminAction();
        action.setAdmin(admin);
        action.setUser(user);
        action.setActionType("BAN");
        action.setReason(reason);
        adminActionRepository.save(action);

        return savedUser;
    }

    @Transactional
    public User reinstateUser(Long userId, Long adminId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        user.setStatus("ACTIVE");
        User savedUser = userRepository.save(user);

        // Log admin action
        AdminAction action = new AdminAction();
        action.setAdmin(admin);
        action.setUser(user);
        action.setActionType("REINSTATE");
        action.setReason(reason);
        adminActionRepository.save(action);

        // Notify user
        notificationService.createNotification(
                userId,
                "Account Reinstated",
                "Your account has been reinstated.",
                "ACCOUNT_REINSTATED"
        );

        return savedUser;
    }

    public List<Paper> getPendingPapers() {
        return paperRepository.findByStatusOrderByCreatedAtDesc("PENDING", org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    public List<Paper> getFlaggedPapers() {
        return paperRepository.findByStatusOrderByCreatedAtDesc("FLAGGED", org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    private void logAdminAction(User admin, Paper paper, String actionType, String reason) {
        AdminAction action = new AdminAction();
        action.setAdmin(admin);
        action.setPaper(paper);
        action.setActionType(actionType);
        action.setReason(reason);
        adminActionRepository.save(action);
    }
}
