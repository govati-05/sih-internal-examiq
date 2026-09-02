package com.examiq.backend.service;

import com.examiq.backend.dto.VerificationResult;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ExamTypeVerificationService {

    public VerificationResult verify(String declaredExamType, String title) {
        String normalizedDeclared = declaredExamType == null ? "" : declaredExamType.trim().toLowerCase(Locale.ROOT);
        String normalizedTitle = title == null ? "" : title.trim().toLowerCase(Locale.ROOT);

        if (normalizedDeclared.isBlank()) {
            return VerificationResult.passed();
        }

        boolean midMismatch = normalizedDeclared.contains("mid")
                && (normalizedTitle.contains("end semester")
                        || normalizedTitle.contains("semester examination")
                        || normalizedTitle.contains("final exam")
                        || normalizedTitle.contains("end-semester"));

        boolean semMismatch = normalizedDeclared.contains("semester")
                && (normalizedTitle.contains("mid semester")
                        || normalizedTitle.contains("unit test")
                        || normalizedTitle.contains("mid-term"));

        if (midMismatch) {
            return VerificationResult.failed("EXAM_TYPE_CHECK",
                    "You marked this as a Mid-Semester paper, but the content and title match a Semester exam. Please confirm the exam type before resubmitting.",
                    0.0);
        }

        if (semMismatch) {
            return VerificationResult.failed("EXAM_TYPE_CHECK",
                    "The file appears to be a Mid-Semester paper, but you marked it as a Semester paper. Please confirm the exam type before resubmitting.",
                    0.0);
        }

        return VerificationResult.passed();
    }
}
