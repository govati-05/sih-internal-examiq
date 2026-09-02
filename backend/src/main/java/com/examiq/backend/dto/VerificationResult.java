package com.examiq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResult {
    private boolean passed;
    private String stage;
    private String message;
    private double score;

    public static VerificationResult passed() {
        return new VerificationResult(true, "OK", null, 1.0);
    }

    public static VerificationResult failed(String stage, String message, double score) {
        return new VerificationResult(false, stage, message, score);
    }
}
