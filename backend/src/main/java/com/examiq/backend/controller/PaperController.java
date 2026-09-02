package com.examiq.backend.controller;

import com.examiq.backend.dto.ApiResponse;
import com.examiq.backend.dto.PaperDto;
import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Subject;
import com.examiq.backend.repository.PaperRepository;
import com.examiq.backend.repository.SubjectRepository;
import com.examiq.backend.service.PaperService;
import com.examiq.backend.service.RepeatedQuestionAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaperController {

    private final PaperService paperService;
    private final RepeatedQuestionAnalysisService repeatedQuestionAnalysisService;
    private final SubjectRepository subjectRepository;
    private final PaperRepository paperRepository;

    public PaperController(PaperService paperService,
            RepeatedQuestionAnalysisService repeatedQuestionAnalysisService,
            SubjectRepository subjectRepository,
            PaperRepository paperRepository) {
        this.paperService = paperService;
        this.repeatedQuestionAnalysisService = repeatedQuestionAnalysisService;
        this.subjectRepository = subjectRepository;
        this.paperRepository = paperRepository;
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
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "studentYear", required = false) String studentYear,
            @RequestParam(value = "accessType", required = false) String accessType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : null;
        Integer parsedYear = parseOptionalYear(year, "Year");
        Integer parsedStudentYear = parseOptionalYear(studentYear, "Academic year");
        return ResponseEntity.ok(ApiResponse.success("Paper uploaded successfully",
                paperService.uploadPaper(file, title, subject, university, parsedYear, examType, author, username,
                        parsedStudentYear, accessType)));
    }

    private Integer parseOptionalYear(String yearValue, String fieldName) {
        if (yearValue == null || yearValue.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(yearValue.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number");
        }
    }

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listSubjects() {
        List<Map<String, Object>> subjects = subjectRepository.findAll().stream()
                .map(s -> {
                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("id", s.getId());
                    item.put("name", s.getCanonicalName() != null ? s.getCanonicalName() : s.getName());
                    return item;
                })
                .sorted((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Subjects loaded", subjects));
    }

    @GetMapping("/papers/search")
    public ResponseEntity<ApiResponse<List<PaperDto>>> search(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "year", required = false) Integer studentYear,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Search results loaded",
                paperService.searchPapers(query, studentYear, subject, sort)));
    }

    @GetMapping("/papers/trending")
    public ResponseEntity<ApiResponse<List<PaperDto>>> trending(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Trending resources loaded",
                paperService.getTrendingPapers(limit)));
    }

    @GetMapping("/papers/{id}")
    public ResponseEntity<ApiResponse<PaperDto>> getPaperById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Paper loaded", paperService.getPaperById(id)));
    }

    @GetMapping("/papers/{id}/repeated-questions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> repeatedQuestionsForPaper(@PathVariable Long id) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));
        if (paper.getSubject() == null) {
            return ResponseEntity.ok(ApiResponse.success("Repeated question analysis loaded", List.of()));
        }
        return ResponseEntity.ok(ApiResponse.success("Repeated question analysis loaded",
                repeatedQuestionAnalysisService.getRepeatedQuestions(paper.getSubject(), 10)));
    }

    @GetMapping("/subjects/{subjectId}/repeated-questions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> repeatedQuestionsForSubject(
            @PathVariable Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        return ResponseEntity.ok(ApiResponse.success("Repeated question analysis loaded",
                repeatedQuestionAnalysisService.getRepeatedQuestions(subject, 10)));
    }
}
