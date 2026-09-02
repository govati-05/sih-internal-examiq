package com.examiq.backend.controller;

import com.examiq.backend.dto.ApiResponse;
import com.examiq.backend.entity.Rating;
import com.examiq.backend.security.AuthenticatedUserResolver;
import com.examiq.backend.service.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RatingController {

    private final RatingService ratingService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public RatingController(RatingService ratingService, AuthenticatedUserResolver authenticatedUserResolver) {
        this.ratingService = ratingService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/papers/{paperId}/rate")
    @PreAuthorize("hasRole('STUDENT') or hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Rating>> ratePaper(
            @PathVariable Long paperId,
            @RequestParam Integer score,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        Long userId = authenticatedUserResolver.getCurrentUser(authentication).getId();
        Rating rating = ratingService.ratePaper(paperId, userId, score, comment);
        return ResponseEntity.ok(ApiResponse.success("Rating submitted successfully", rating));
    }

    @GetMapping("/papers/{paperId}/ratings")
    public ResponseEntity<ApiResponse<List<Rating>>> getPaperRatings(@PathVariable Long paperId) {
        List<Rating> ratings = ratingService.getPaperRatings(paperId);
        return ResponseEntity.ok(ApiResponse.success("Ratings retrieved successfully", ratings));
    }

    @GetMapping("/papers/{paperId}/average-rating")
    public ResponseEntity<ApiResponse<Double>> getAverageRating(@PathVariable Long paperId) {
        Double averageRating = ratingService.getAverageRating(paperId);
        return ResponseEntity.ok(ApiResponse.success("Average rating retrieved successfully", averageRating));
    }
}
