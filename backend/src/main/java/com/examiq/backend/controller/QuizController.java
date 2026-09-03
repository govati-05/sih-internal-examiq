package com.examiq.backend.controller;

import com.examiq.backend.dto.ApiResponse;
import com.examiq.backend.entity.User;
import com.examiq.backend.security.AuthenticatedUserResolver;
import com.examiq.backend.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public QuizController(QuizService quizService, AuthenticatedUserResolver authenticatedUserResolver) {
        this.quizService = quizService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    public record GenerateRequest(Long subjectId, Integer studentYear, Integer numQuestions, String difficulty) {
    }

    public record SubmitRequest(List<Integer> answers) {
    }

    @PostMapping("/generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generate(@RequestBody GenerateRequest request) {
        User user = authenticatedUserResolver.getCurrentUser();
        if (request.subjectId() == null) {
            throw new IllegalArgumentException("A subject is required to generate a quiz");
        }
        Map<String, Object> quiz = quizService.generateQuiz(user, request.subjectId(), request.studentYear(),
                request.numQuestions(), request.difficulty());
        return ResponseEntity.ok(ApiResponse.success("Quiz generated", quiz));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submit(@PathVariable Long id,
            @RequestBody SubmitRequest request) {
        User user = authenticatedUserResolver.getCurrentUser();
        Map<String, Object> result = quizService.submitQuiz(user, id, request.answers());
        return ResponseEntity.ok(ApiResponse.success("Quiz submitted", result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttempt(@PathVariable Long id) {
        User user = authenticatedUserResolver.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Quiz loaded", quizService.getAttempt(user, id)));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> history() {
        User user = authenticatedUserResolver.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Quiz history loaded", quizService.getHistory(user)));
    }
}
