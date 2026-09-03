package com.examiq.backend.service;

import com.examiq.backend.entity.*;
import com.examiq.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FacultyService {

        private final PaperRepository paperRepository;
        private final UserRepository userRepository;
        private final UniversityRepository universityRepository;
        private final FacultyVerificationRepository facultyVerificationRepository;
        private final QuestionRepository questionRepository;
        private final RatingRepository ratingRepository;
        private final NotificationService notificationService;

        public FacultyService(PaperRepository paperRepository, UserRepository userRepository,
                        UniversityRepository universityRepository,
                        FacultyVerificationRepository facultyVerificationRepository,
                        QuestionRepository questionRepository, RatingRepository ratingRepository,
                        NotificationService notificationService) {
                this.paperRepository = paperRepository;
                this.userRepository = userRepository;
                this.universityRepository = universityRepository;
                this.facultyVerificationRepository = facultyVerificationRepository;
                this.questionRepository = questionRepository;
                this.ratingRepository = ratingRepository;
                this.notificationService = notificationService;
        }

        @Transactional
        public FacultyVerification submitVerificationRequest(Long facultyId, Long universityId, String documentsUrl) {
                User faculty = userRepository.findById(facultyId)
                                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));

                University university = universityId != null ? universityRepository.findById(universityId).orElse(null)
                                : null;

                FacultyVerification verification = new FacultyVerification();
                verification.setFaculty(faculty);
                verification.setUniversity(university);
                verification.setVerificationStatus("PENDING");
                verification.setDocumentsUrl(documentsUrl);
                return facultyVerificationRepository.save(verification);
        }

        @Transactional
        public FacultyVerification approveFacultyVerification(Long verificationId, Long adminId) {
                FacultyVerification verification = facultyVerificationRepository.findById(verificationId)
                                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));

                User admin = userRepository.findById(adminId)
                                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

                verification.setVerificationStatus("APPROVED");
                verification.setReviewedBy(admin);
                verification.setReviewedAt(java.time.LocalDateTime.now());
                FacultyVerification saved = facultyVerificationRepository.save(verification);

                // Notify faculty
                notificationService.createNotification(
                                verification.getFaculty().getId(),
                                "Faculty Verification Approved",
                                "Your faculty verification has been approved. You can now upload official papers.",
                                "FACULTY_VERIFIED");

                return saved;
        }

        public Map<String, Object> getFacultyAnalytics(Long facultyId) {
                User faculty = userRepository.findById(facultyId)
                                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));

                List<Paper> uploadedPapers = paperRepository.findByUploaderOrderByCreatedAtDesc(faculty);

                // Topic frequency analysis
                Map<String, Long> topicFrequency = uploadedPapers.stream()
                                .collect(Collectors.groupingBy(
                                                p -> p.getSubject() != null ? p.getSubject().getCanonicalName()
                                                                : "Unknown",
                                                Collectors.counting()));

                // Difficulty distribution
                List<Question> questions = uploadedPapers.stream()
                                .flatMap(paper -> questionRepository.findByPaper(paper).stream())
                                .toList();

                Map<String, Long> difficultyDistribution = questions.stream()
                                .collect(Collectors.groupingBy(
                                                q -> q.getDifficultyLevel() != null ? q.getDifficultyLevel()
                                                                : "UNKNOWN",
                                                Collectors.counting()));

                // Average ratings
                double averageRating = uploadedPapers.stream()
                                .mapToDouble(paper -> {
                                        List<Rating> ratings = ratingRepository.findByPaper(paper);
                                        return ratings.isEmpty() ? 0
                                                        : ratings.stream().mapToInt(Rating::getScore).average()
                                                                        .orElse(0);
                                })
                                .average()
                                .orElse(0);

                return Map.of(
                                "totalUploads", uploadedPapers.size(),
                                "topicFrequency", topicFrequency,
                                "difficultyDistribution", difficultyDistribution,
                                "averageRating", averageRating,
                                "totalQuestions", questions.size());
        }

        public List<Paper> getFacultyUploads(Long facultyId) {
                User faculty = userRepository.findById(facultyId)
                                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));
                return paperRepository.findByUploaderOrderByCreatedAtDesc(faculty);
        }

        /** Real counts only - no fabricated demo numbers. */
        public Map<String, Object> getFacultyDashboardStats(Long facultyId) {
                User faculty = userRepository.findById(facultyId)
                                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));
                List<Paper> uploads = getFacultyUploads(facultyId);
                long downloads = uploads.stream()
                                .mapToLong(p -> p.getDownloadCount() == null ? 0L : p.getDownloadCount())
                                .sum();
                String verificationStatus = facultyVerificationRepository.findByFaculty(faculty)
                                .map(FacultyVerification::getVerificationStatus)
                                .orElse("NOT_SUBMITTED");

                return Map.of(
                                "papersUploaded", uploads.size(),
                                "downloads", downloads,
                                "verification", verificationStatus);
        }

        public List<FacultyVerification> getPendingVerifications() {
                return facultyVerificationRepository.findByVerificationStatus("PENDING");
        }
}
