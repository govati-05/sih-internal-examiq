package com.examiq.backend.controller;

import com.examiq.backend.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/util")
public class UtilController {

    private final PasswordEncoder passwordEncoder;

    public UtilController(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/hash")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> generateHash(@RequestBody Map<String, String> payload) {
        String password = payload != null ? payload.get("password") : null;
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        return ResponseEntity.ok(ApiResponse.success("Hash generated", passwordEncoder.encode(password)));
    }
}
