package com.examiq.backend.service;

import com.examiq.backend.dto.VerificationResult;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DocumentTypeVerificationService {

    public VerificationResult verify(String title) {
        String normalizedTitle = title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
        if (normalizedTitle.isBlank()) {
            return VerificationResult.failed("DOCUMENT_TYPE_CHECK",
                    "This does not look like a question paper. Please upload the correct file.", 0.0);
        }

        boolean assignmentLike = normalizedTitle.contains("assignment")
                || normalizedTitle.contains("submit by")
                || normalizedTitle.contains("lab manual")
                || normalizedTitle.contains("due date")
                || normalizedTitle.contains("roll no")
                || normalizedTitle.contains("syllabus")
                || normalizedTitle.contains("notes");

        boolean questionLike = normalizedTitle.contains("question")
                || normalizedTitle.contains("answer any")
                || normalizedTitle.contains("attempt all")
                || normalizedTitle.contains("max marks")
                || normalizedTitle.contains("time")
                || normalizedTitle.contains("section a")
                || normalizedTitle.contains("section b")
                || normalizedTitle.contains("mid semester")
                || normalizedTitle.contains("end semester")
                || normalizedTitle.contains("semester examination")
                || normalizedTitle.contains("exam");

        if (assignmentLike && !questionLike) {
            return VerificationResult.failed("DOCUMENT_TYPE_CHECK",
                    "This doesn't look like a question paper — it appears to be an assignment or notes. Please upload the correct file.",
                    0.0);
        }

        if (!questionLike && !normalizedTitle.contains("exam")) {
            return VerificationResult.failed("DOCUMENT_TYPE_CHECK",
                    "This doesn't look like a question paper. Please upload the correct file.", 0.0);
        }

        return VerificationResult.passed();
    }
}
