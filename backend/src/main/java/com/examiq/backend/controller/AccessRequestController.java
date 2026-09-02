package com.examiq.backend.controller;

import com.examiq.backend.dto.ApiResponse;
import com.examiq.backend.entity.AccessRequest;
import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.User;
import com.examiq.backend.repository.PaperRepository;
import com.examiq.backend.security.AuthenticatedUserResolver;
import com.examiq.backend.service.AccessRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AccessRequestController {

    private final AccessRequestService accessRequestService;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final PaperRepository paperRepository;

    public AccessRequestController(AccessRequestService accessRequestService,
            AuthenticatedUserResolver authenticatedUserResolver,
            PaperRepository paperRepository) {
        this.accessRequestService = accessRequestService;
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.paperRepository = paperRepository;
    }

    @PostMapping("/papers/{paperId}/access-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestAccess(
            @PathVariable Long paperId,
            @RequestBody Map<String, String> payload) {
        User requester = authenticatedUserResolver.getCurrentUser();
        AccessRequest request = accessRequestService.createRequest(paperId, requester,
                payload.get("permissionLevel"), payload.get("message"));
        return ResponseEntity.ok(ApiResponse.success("Access request submitted", accessRequestService.toDto(request)));
    }

    @GetMapping("/papers/{paperId}/access-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRequestsForPaper(@PathVariable Long paperId) {
        User user = authenticatedUserResolver.getCurrentUser();
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));
        List<Map<String, Object>> requests = accessRequestService.getRequestsForPaper(paper, user).stream()
                .map(accessRequestService::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Access requests loaded", requests));
    }

    @GetMapping("/access-requests/owner")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllRequestsForOwner() {
        User owner = authenticatedUserResolver.getCurrentUser();
        List<Map<String, Object>> requests = accessRequestService.getRequestsForOwner(owner).stream()
                .map(accessRequestService::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Access requests loaded", requests));
    }

    @GetMapping("/access-requests/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyRequests() {
        User requester = authenticatedUserResolver.getCurrentUser();
        List<Map<String, Object>> requests = accessRequestService.getMyRequests(requester).stream()
                .map(accessRequestService::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Your access requests loaded", requests));
    }

    @GetMapping("/papers/{paperId}/access-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccessStatus(@PathVariable Long paperId) {
        User user = authenticatedUserResolver.getCurrentUser();
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));
        String level = accessRequestService.resolveAccessLevel(paper, user);
        String latestRequestStatus = accessRequestService.getLatestRequestStatus(paper, user);
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("accessType", paper.getAccessType() != null ? paper.getAccessType() : "PUBLIC");
        status.put("accessLevel", level);
        status.put("isOwner", paper.getUploader() != null && paper.getUploader().getId().equals(user.getId()));
        status.put("latestRequestStatus", latestRequestStatus);
        return ResponseEntity.ok(ApiResponse.success("Access status loaded", status));
    }

    @PutMapping("/access-requests/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(@PathVariable Long id) {
        User user = authenticatedUserResolver.getCurrentUser();
        AccessRequest request = accessRequestService.decide(id, user, true);
        return ResponseEntity.ok(ApiResponse.success("Access request approved", accessRequestService.toDto(request)));
    }

    @PutMapping("/access-requests/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(@PathVariable Long id) {
        User user = authenticatedUserResolver.getCurrentUser();
        AccessRequest request = accessRequestService.decide(id, user, false);
        return ResponseEntity.ok(ApiResponse.success("Access request rejected", accessRequestService.toDto(request)));
    }
}
