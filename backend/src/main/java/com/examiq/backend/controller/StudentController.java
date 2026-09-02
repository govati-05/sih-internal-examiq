package com.examiq.backend.controller;

import com.examiq.backend.dto.ApiResponse;
import com.examiq.backend.entity.User;
import com.examiq.backend.service.StudentDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StudentController {

    private final StudentDashboardService studentDashboardService;

    public StudentController(StudentDashboardService studentDashboardService) {
        this.studentDashboardService = studentDashboardService;
    }

    @GetMapping("/student/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity
                .ok(ApiResponse.success("Dashboard loaded", studentDashboardService.getDashboard(currentUser())));
    }

    @GetMapping("/student/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> profile() {
        return ResponseEntity
                .ok(ApiResponse.success("Profile loaded", studentDashboardService.getProfile(currentUser())));
    }

    @PutMapping("/student/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated",
                studentDashboardService.updateProfile(currentUser(), payload)));
    }

    @GetMapping("/student/bookmarks")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> bookmarks() {
        return ResponseEntity
                .ok(ApiResponse.success("Bookmarks loaded", studentDashboardService.getBookmarks(currentUser())));
    }

    @PostMapping("/student/bookmarks/{paperId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addBookmark(@PathVariable Long paperId) {
        return ResponseEntity.ok(ApiResponse.success("Bookmark saved",
                studentDashboardService.addBookmark(currentUser(), paperId)));
    }

    @DeleteMapping("/student/bookmarks/{paperId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeBookmark(@PathVariable Long paperId) {
        return ResponseEntity.ok(ApiResponse.success("Bookmark removed",
                studentDashboardService.removeBookmark(currentUser(), paperId)));
    }

    @GetMapping("/student/uploads")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> myUploads() {
        return ResponseEntity
                .ok(ApiResponse.success("Uploads loaded", studentDashboardService.getMyUploads(currentUser())));
    }

    @GetMapping("/student/notifications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> notifications() {
        return ResponseEntity.ok(
                ApiResponse.success("Notifications loaded", studentDashboardService.getNotifications(currentUser())));
    }

    @PutMapping("/student/notifications/{notificationId}/read")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markNotificationRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read",
                studentDashboardService.markNotificationRead(currentUser(), notificationId)));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : null;
        return studentDashboardService.getAuthenticatedUser(username);
    }
}
