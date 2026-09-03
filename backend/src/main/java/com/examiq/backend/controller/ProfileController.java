package com.examiq.backend.controller;

import com.examiq.backend.dto.ApiResponse;
import com.examiq.backend.entity.User;
import com.examiq.backend.security.AuthenticatedUserResolver;
import com.examiq.backend.service.ProfilePictureService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Profile picture management shared across all roles (student, faculty,
 * admin) - kept separate from /api/student/** which is student-only.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfilePictureService profilePictureService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ProfileController(ProfilePictureService profilePictureService,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.profilePictureService = profilePictureService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadPicture(@RequestParam("file") MultipartFile file) {
        User user = authenticatedUserResolver.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Profile picture updated", profilePictureService.upload(user, file)));
    }

    @DeleteMapping("/picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> removePicture() {
        User user = authenticatedUserResolver.getCurrentUser();
        profilePictureService.remove(user);
        return ResponseEntity.ok(ApiResponse.success("Profile picture removed", null));
    }
}
