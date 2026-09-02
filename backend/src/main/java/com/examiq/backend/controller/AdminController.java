package com.examiq.backend.controller;

import com.examiq.backend.dto.ApiResponse;
import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.User;
import com.examiq.backend.security.AuthenticatedUserResolver;
import com.examiq.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AdminController {

    private final AdminService adminService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminController(AdminService adminService, AuthenticatedUserResolver authenticatedUserResolver) {
        this.adminService = adminService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard loaded", Map.of(
                "totalUsers", 1248,
                "students", 1104,
                "faculty", 122,
                "approvedPapers", 845)));
    }

    @PutMapping("/admin/papers/{paperId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Paper>> approvePaper(
            @PathVariable Long paperId,
            @RequestParam(required = false) String reason) {
        Long adminId = authenticatedUserResolver.getCurrentUser().getId();
        Paper paper = adminService.approvePaper(paperId, adminId, reason);
        return ResponseEntity.ok(ApiResponse.success("Paper approved successfully", paper));
    }

    @PutMapping("/admin/papers/{paperId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Paper>> rejectPaper(
            @PathVariable Long paperId,
            @RequestParam String reason) {
        Long adminId = authenticatedUserResolver.getCurrentUser().getId();
        Paper paper = adminService.rejectPaper(paperId, adminId, reason);
        return ResponseEntity.ok(ApiResponse.success("Paper rejected successfully", paper));
    }

    @PutMapping("/admin/papers/{paperId}/reupload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Paper>> requestReupload(
            @PathVariable Long paperId,
            @RequestParam String reason) {
        Long adminId = authenticatedUserResolver.getCurrentUser().getId();
        Paper paper = adminService.requestReupload(paperId, adminId, reason);
        return ResponseEntity.ok(ApiResponse.success("Re-upload requested successfully", paper));
    }

    @GetMapping("/admin/papers/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Paper>>> getPendingPapers() {
        List<Paper> papers = adminService.getPendingPapers();
        return ResponseEntity.ok(ApiResponse.success("Pending papers retrieved successfully", papers));
    }

    @GetMapping("/admin/papers/flagged")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Paper>>> getFlaggedPapers() {
        List<Paper> papers = adminService.getFlaggedPapers();
        return ResponseEntity.ok(ApiResponse.success("Flagged papers retrieved successfully", papers));
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @PutMapping("/admin/users/{userId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> suspendUser(
            @PathVariable Long userId,
            @RequestParam String reason) {
        Long adminId = authenticatedUserResolver.getCurrentUser().getId();
        User user = adminService.suspendUser(userId, adminId, reason);
        return ResponseEntity.ok(ApiResponse.success("User suspended successfully", user));
    }

    @PutMapping("/admin/users/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> banUser(
            @PathVariable Long userId,
            @RequestParam String reason) {
        Long adminId = authenticatedUserResolver.getCurrentUser().getId();
        User user = adminService.banUser(userId, adminId, reason);
        return ResponseEntity.ok(ApiResponse.success("User banned successfully", user));
    }

    @PutMapping("/admin/users/{userId}/reinstate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> reinstateUser(
            @PathVariable Long userId,
            @RequestParam String reason) {
        Long adminId = authenticatedUserResolver.getCurrentUser().getId();
        User user = adminService.reinstateUser(userId, adminId, reason);
        return ResponseEntity.ok(ApiResponse.success("User reinstated successfully", user));
    }
}
