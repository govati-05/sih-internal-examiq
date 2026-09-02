package com.examiq.backend.controller;

import com.examiq.backend.dto.ApiResponse;
import com.examiq.backend.entity.Notification;
import com.examiq.backend.security.AuthenticatedUserResolver;
import com.examiq.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public NotificationController(NotificationService notificationService,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.notificationService = notificationService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('STUDENT') or hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Notification>>> getUserNotifications() {
        Long userId = authenticatedUserResolver.getCurrentUser().getId();
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }

    @GetMapping("/notifications/unread")
    @PreAuthorize("hasRole('STUDENT') or hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Notification>>> getUnreadNotifications() {
        Long userId = authenticatedUserResolver.getCurrentUser().getId();
        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success("Unread notifications retrieved successfully", notifications));
    }

    @PutMapping("/notifications/{notificationId}/read")
    @PreAuthorize("hasRole('STUDENT') or hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PutMapping("/notifications/read-all")
    @PreAuthorize("hasRole('STUDENT') or hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        Long userId = authenticatedUserResolver.getCurrentUser().getId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}
