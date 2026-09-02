package com.examiq.backend.controller;

import com.examiq.backend.dto.ApiResponse;
import com.examiq.backend.dto.PaperDto;
import com.examiq.backend.service.PaperService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static java.util.Objects.requireNonNullElse;

@RestController
@RequestMapping("/api")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    @PostMapping("/papers/upload")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PaperDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "university", required = false) String university,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam(value = "examType", required = false) String examType,
            @RequestParam(value = "author", required = false) String author) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : null;
        Integer parsedYear = parseOptionalYear(year);
        return ResponseEntity.ok(ApiResponse.success("Paper uploaded successfully",
                paperService.uploadPaper(file, title, subject, university, parsedYear, examType, author, username)));
    }

    private Integer parseOptionalYear(String yearValue) {
        if (yearValue == null || yearValue.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(yearValue.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Year must be a valid number");
        }
    }

    @GetMapping("/papers/search")
    public ResponseEntity<ApiResponse<List<PaperDto>>> search(
            @RequestParam(value = "q", required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success("Search results loaded",
                paperService.searchPapers(query)));
    }

    @GetMapping("/papers/{id}")
    public ResponseEntity<ApiResponse<PaperDto>> getPaperById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Paper loaded", paperService.getPaperById(id)));
    }
}
