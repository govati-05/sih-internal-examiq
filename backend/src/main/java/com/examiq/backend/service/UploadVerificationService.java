package com.examiq.backend.service;

import com.examiq.backend.dto.VerificationResult;
import org.springframework.stereotype.Service;

@Service
public class UploadVerificationService {

    private final DocumentTypeVerificationService documentTypeVerificationService;
    private final ExamTypeVerificationService examTypeVerificationService;

    public UploadVerificationService(DocumentTypeVerificationService documentTypeVerificationService,
            ExamTypeVerificationService examTypeVerificationService) {
        this.documentTypeVerificationService = documentTypeVerificationService;
        this.examTypeVerificationService = examTypeVerificationService;
    }

    public VerificationResult verifyUpload(String title, String examType) {
        VerificationResult docCheck = documentTypeVerificationService.verify(title);
        if (!docCheck.isPassed()) {
            return docCheck;
        }

        VerificationResult examCheck = examTypeVerificationService.verify(examType, title);
        if (!examCheck.isPassed()) {
            return examCheck;
        }

        return VerificationResult.passed();
    }
}
