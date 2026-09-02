package com.examiq.backend.controller;

import com.examiq.backend.dto.ApiResponse;
import com.examiq.backend.entity.Bookmark;
import com.examiq.backend.security.AuthenticatedUserResolver;
import com.examiq.backend.service.BookmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public BookmarkController(BookmarkService bookmarkService, AuthenticatedUserResolver authenticatedUserResolver) {
        this.bookmarkService = bookmarkService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/papers/{paperId}/bookmark")
    @PreAuthorize("hasRole('STUDENT') or hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<Bookmark>> addBookmark(@PathVariable Long paperId) {
        Long userId = authenticatedUserResolver.getCurrentUser().getId();
        Bookmark bookmark = bookmarkService.addBookmark(paperId, userId);
        return ResponseEntity.ok(ApiResponse.success("Paper bookmarked successfully", bookmark));
    }

    @DeleteMapping("/papers/{paperId}/bookmark")
    @PreAuthorize("hasRole('STUDENT') or hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<Void>> removeBookmark(@PathVariable Long paperId) {
        Long userId = authenticatedUserResolver.getCurrentUser().getId();
        bookmarkService.removeBookmark(paperId, userId);
        return ResponseEntity.ok(ApiResponse.success("Bookmark removed successfully", null));
    }

    @GetMapping("/papers/{paperId}/is-bookmarked")
    @PreAuthorize("hasRole('STUDENT') or hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<Boolean>> isBookmarked(@PathVariable Long paperId) {
        Long userId = authenticatedUserResolver.getCurrentUser().getId();
        boolean bookmarked = bookmarkService.isBookmarked(paperId, userId);
        return ResponseEntity.ok(ApiResponse.success("Bookmark status retrieved successfully", bookmarked));
    }
}
