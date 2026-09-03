package com.examiq.backend.service;

import com.examiq.backend.entity.AccessRequest;
import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.User;
import com.examiq.backend.repository.AccessRequestRepository;
import com.examiq.backend.repository.PaperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AccessRequestService {

    private static final Set<String> VALID_PERMISSIONS = Set.of("VIEW", "VIEW_DOWNLOAD");

    private final AccessRequestRepository accessRequestRepository;
    private final PaperRepository paperRepository;
    private final NotificationService notificationService;
    private final ContributorScoreService contributorScoreService;

    public AccessRequestService(AccessRequestRepository accessRequestRepository,
            PaperRepository paperRepository,
            NotificationService notificationService,
            ContributorScoreService contributorScoreService) {
        this.accessRequestRepository = accessRequestRepository;
        this.paperRepository = paperRepository;
        this.notificationService = notificationService;
        this.contributorScoreService = contributorScoreService;
    }

    @Transactional
    public AccessRequest createRequest(Long paperId, User requester, String permissionLevel, String message) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        if (!"APPROVED".equalsIgnoreCase(paper.getStatus())) {
            throw new IllegalArgumentException("This resource is not available");
        }
        if (!"REQUEST_ACCESS".equalsIgnoreCase(paper.getAccessType())) {
            throw new IllegalArgumentException("This resource is public and does not require access requests");
        }
        if (paper.getUploader() != null && paper.getUploader().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("You already own this resource");
        }

        String normalizedPermission = permissionLevel == null ? "VIEW" : permissionLevel.trim().toUpperCase();
        if (!VALID_PERMISSIONS.contains(normalizedPermission)) {
            throw new IllegalArgumentException("Invalid permission level requested");
        }

        boolean hasPending = accessRequestRepository.findTopByPaperAndRequesterOrderByCreatedAtDesc(paper, requester)
                .map(existing -> "PENDING".equalsIgnoreCase(existing.getStatus())
                        || "APPROVED".equalsIgnoreCase(existing.getStatus()))
                .orElse(false);
        if (hasPending) {
            throw new IllegalArgumentException("You already have a pending or approved request for this resource");
        }

        AccessRequest request = new AccessRequest();
        request.setPaper(paper);
        request.setRequester(requester);
        request.setPermissionLevel(normalizedPermission);
        request.setMessage(message);
        request.setStatus("PENDING");
        AccessRequest saved = accessRequestRepository.save(request);

        if (paper.getUploader() != null) {
            notificationService.createNotification(paper.getUploader().getId(),
                    "New Access Request",
                    requester.getFullName() + " requested " + normalizedPermission.replace("_", " + ")
                            + " access to '" + paper.getTitle() + "'",
                    "ACCESS_REQUEST");
        }
        return saved;
    }

    @Transactional
    public AccessRequest decide(Long requestId, User decidingUser, boolean approve) {
        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Access request not found"));

        Paper paper = request.getPaper();
        boolean isOwner = paper.getUploader() != null && paper.getUploader().getId().equals(decidingUser.getId());
        boolean isAdmin = decidingUser.getRole() != null && "ADMIN".equalsIgnoreCase(decidingUser.getRole().getName());
        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("You are not authorized to manage this request");
        }
        if (request.getRequester() != null && request.getRequester().getId().equals(decidingUser.getId())) {
            throw new IllegalArgumentException("You cannot approve or reject your own access request");
        }
        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalArgumentException("This request has already been decided");
        }

        request.setStatus(approve ? "APPROVED" : "REJECTED");
        request.setDecidedBy(decidingUser);
        AccessRequest saved = accessRequestRepository.save(request);

        notificationService.createNotification(request.getRequester().getId(),
                approve ? "Access Request Approved" : "Access Request Rejected",
                "Your request for '" + paper.getTitle() + "' was " + (approve ? "approved" : "rejected") + ".",
                approve ? "ACCESS_APPROVED" : "ACCESS_REJECTED");

        if (approve && paper.getUploader() != null) {
            contributorScoreService.addPoints(paper.getUploader().getId(), 1);
        }

        return saved;
    }

    public List<AccessRequest> getRequestsForOwner(User owner) {
        return accessRequestRepository.findByPaper_UploaderOrderByCreatedAtDesc(owner);
    }

    public List<AccessRequest> getRequestsForPaper(Paper paper, User owner) {
        boolean isOwner = paper.getUploader() != null && paper.getUploader().getId().equals(owner.getId());
        boolean isAdmin = owner.getRole() != null && "ADMIN".equalsIgnoreCase(owner.getRole().getName());
        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("You are not authorized to view these requests");
        }
        return accessRequestRepository.findByPaperOrderByCreatedAtDesc(paper);
    }

    public List<AccessRequest> getMyRequests(User requester) {
        return accessRequestRepository.findByRequesterOrderByCreatedAtDesc(requester);
    }

    /**
     * Highest permission the given user currently holds for a paper: NONE, VIEW, or VIEW_DOWNLOAD.
     */
    public String resolveAccessLevel(Paper paper, User user) {
        if (paper.getUploader() != null && user != null && paper.getUploader().getId().equals(user.getId())) {
            return "VIEW_DOWNLOAD";
        }
        if (user != null && user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName())) {
            return "VIEW_DOWNLOAD";
        }
        if (!"REQUEST_ACCESS".equalsIgnoreCase(paper.getAccessType())) {
            return "VIEW_DOWNLOAD";
        }
        if (user == null) {
            return "NONE";
        }
        List<AccessRequest> approved = accessRequestRepository.findByPaperAndRequesterAndStatus(paper, user,
                "APPROVED");
        return approved.stream()
                .anyMatch(r -> "VIEW_DOWNLOAD".equalsIgnoreCase(r.getPermissionLevel())) ? "VIEW_DOWNLOAD"
                        : approved.isEmpty() ? "NONE" : "VIEW";
    }

    public String getLatestRequestStatus(Paper paper, User user) {
        if (user == null) {
            return null;
        }
        return accessRequestRepository.findTopByPaperAndRequesterOrderByCreatedAtDesc(paper, user)
                .map(AccessRequest::getStatus)
                .orElse(null);
    }

    public Map<String, Object> toDto(AccessRequest request) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", request.getId());
        dto.put("paperId", request.getPaper().getId());
        dto.put("paperTitle", request.getPaper().getTitle());
        dto.put("requesterId", request.getRequester().getId());
        dto.put("requesterName", request.getRequester().getFullName());
        dto.put("requesterBranch", request.getRequester().getBranch());
        dto.put("requesterYear", request.getRequester().getYear());
        dto.put("permissionLevel", request.getPermissionLevel());
        dto.put("message", request.getMessage());
        dto.put("status", request.getStatus());
        dto.put("createdAt", request.getCreatedAt());
        dto.put("updatedAt", request.getUpdatedAt());
        return dto;
    }
}
