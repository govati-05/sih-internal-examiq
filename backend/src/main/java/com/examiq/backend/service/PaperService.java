package com.examiq.backend.service;

import com.examiq.backend.dto.PaperDto;
import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Subject;
import com.examiq.backend.entity.SubjectAlias;
import com.examiq.backend.entity.University;
import com.examiq.backend.entity.Upload;
import com.examiq.backend.entity.User;
import com.examiq.backend.entity.VerificationLog;
import com.examiq.backend.repository.PaperRepository;
import com.examiq.backend.repository.RatingRepository;
import com.examiq.backend.repository.SubjectAliasRepository;
import com.examiq.backend.repository.SubjectRepository;
import com.examiq.backend.repository.UniversityRepository;
import com.examiq.backend.repository.UploadRepository;
import com.examiq.backend.repository.UserRepository;
import com.examiq.backend.repository.VerificationLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PaperService {

    private final PaperRepository paperRepository;
    private final SubjectRepository subjectRepository;
    private final UniversityRepository universityRepository;
    private final UserRepository userRepository;
    private final UploadRepository uploadRepository;
    private final RatingRepository ratingRepository;
    private final NotificationService notificationService;
    private final SubjectAliasRepository subjectAliasRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final UploadVerificationService uploadVerificationService;

    @Value("${app.storage.path:./storage}")
    private String storagePath;

    @Value("${app.ai.service-url:http://localhost:8001}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate;

    public PaperService(PaperRepository paperRepository,
            SubjectRepository subjectRepository,
            UniversityRepository universityRepository,
            UserRepository userRepository,
            UploadRepository uploadRepository,
            RatingRepository ratingRepository,
            NotificationService notificationService,
            SubjectAliasRepository subjectAliasRepository,
            VerificationLogRepository verificationLogRepository,
            UploadVerificationService uploadVerificationService,
            RestTemplate restTemplate) {
        this.paperRepository = paperRepository;
        this.subjectRepository = subjectRepository;
        this.universityRepository = universityRepository;
        this.userRepository = userRepository;
        this.uploadRepository = uploadRepository;
        this.ratingRepository = ratingRepository;
        this.notificationService = notificationService;
        this.subjectAliasRepository = subjectAliasRepository;
        this.verificationLogRepository = verificationLogRepository;
        this.uploadVerificationService = uploadVerificationService;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public PaperDto uploadPaper(MultipartFile file,
            String title,
            String subjectName,
            String universityName,
            Integer year,
            String examType,
            String author,
            String username) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A paper file is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Paper title is required");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("User not authenticated");
        }

        String normalizedSubject = normalizeOptionalText(subjectName, "General");
        String normalizedUniversity = normalizeOptionalText(universityName, "Unknown University");

        User uploader = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Uploader not found"));

        Subject subject = resolveSubject(normalizedSubject);

        University university = universityRepository.findByNameIgnoreCase(normalizedUniversity)
                .orElseGet(() -> {
                    University newUniversity = new University();
                    newUniversity.setName(normalizedUniversity);
                    return universityRepository.save(newUniversity);
                });

        // Check for duplicate papers - accept as REJECTED instead of throwing error
        boolean isDuplicate = paperRepository.existsByTitleAndSubjectAndUniversityAndYearAndExamType(
                title, subject, university, year, examType);

        if (isDuplicate) {
            // Create notification for duplicate attempt
            try {
                notificationService.createNotification(
                        uploader.getId(),
                        "Duplicate Paper Upload",
                        "Your paper '" + title + "' was marked as REJECTED because it already exists in the database.",
                        "DUPLICATE_REJECTED");
            } catch (Exception e) {
                System.err.println("Failed to create notification: " + e.getMessage());
            }
        }

        com.examiq.backend.dto.VerificationResult verificationResult = uploadVerificationService.verifyUpload(title,
                examType);
        if (!verificationResult.isPassed()) {
            Paper rejectedPaper = createRejectedPaperRecord(file, title, subject, university, uploader, year, examType,
                    author, fileHashForRejectedUpload(file));
            createVerificationLog(rejectedPaper, null, verificationResult.getStage(), verificationResult.getScore(),
                    verificationResult.getMessage());
            try {
                String notificationTitle = "Upload Rejected";
                if ("EXAM_TYPE_CHECK".equals(verificationResult.getStage())) {
                    notificationTitle = "Exam Type Mismatch";
                }
                notificationService.createNotification(uploader.getId(), notificationTitle,
                        verificationResult.getMessage(),
                        "UPLOAD_REJECTED");
            } catch (Exception e) {
                System.err.println("Failed to create notification: " + e.getMessage());
            }
            return toDto(rejectedPaper);
        }

        // Check subject relevance using AI service and auto-approve if relevant
        boolean aiApproved = false;
        try {
            String subjectCheckUrl = aiServiceUrl + "/ai/subject-check";
            java.util.Map<String, Object> request = new java.util.HashMap<>();
            request.put("text", title);
            request.put("query", subject.getCanonicalName());

            java.util.Map<String, Object> response = restTemplate.postForObject(subjectCheckUrl, request,
                    java.util.Map.class);
            if (response != null && response.containsKey("data")) {
                java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.get("data");
                Double matchScore = data.get("match_score") instanceof Number
                        ? ((Number) data.get("match_score")).doubleValue()
                        : null;
                boolean obviousMismatch = isObviousSubjectMismatch(title, subject.getCanonicalName());
                if (obviousMismatch) {
                    String rejectionMessage = "Paper does not appear to be relevant to the subject "
                            + subject.getCanonicalName() + ". The uploaded content is for a different subject.";
                    Paper rejectedPaper = createRejectedPaperRecord(file, title, subject, university, uploader, year,
                            examType, author, fileHashForRejectedUpload(file));
                    createVerificationLog(rejectedPaper, null, "SUBJECT_RELEVANCE_CHECK", matchScore,
                            rejectionMessage);
                    try {
                        notificationService.createNotification(uploader.getId(), "Paper Not Related to Subject",
                                rejectionMessage, "SUBJECT_REJECTED");
                    } catch (Exception e) {
                        System.err.println("Failed to create notification: " + e.getMessage());
                    }
                    return toDto(rejectedPaper);
                }
                if (matchScore != null && matchScore >= 0.7) {
                    aiApproved = true;
                } else if (matchScore != null && matchScore < 0.5) {
                    String rejectionMessage = "Paper does not appear to be relevant to the subject "
                            + subject.getCanonicalName() + ". Match score: " + matchScore;
                    Paper rejectedPaper = createRejectedPaperRecord(file, title, subject, university, uploader, year,
                            examType, author, fileHashForRejectedUpload(file));
                    createVerificationLog(rejectedPaper, null, "SUBJECT_RELEVANCE_CHECK", matchScore,
                            rejectionMessage);
                    try {
                        notificationService.createNotification(uploader.getId(), "Paper Not Related to Subject",
                                rejectionMessage, "SUBJECT_REJECTED");
                    } catch (Exception e) {
                        System.err.println("Failed to create notification: " + e.getMessage());
                    }
                    return toDto(rejectedPaper);
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("Warning: AI subject check failed: " + e.getMessage());
        }

        String fileName = System.currentTimeMillis() + "_"
                + Objects.requireNonNull(file.getOriginalFilename()).replaceAll("\\s+", "_");
        Path path = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
            Path target = path.resolve(fileName);
            Files.copy(file.getInputStream(), target);

            // Calculate SHA-256 hash of the file
            String fileHash = calculateFileHash(target);

            // Check for exact duplicate by file hash
            boolean isFileDuplicate = paperRepository.existsByFileHash(fileHash);
            if (isFileDuplicate) {
                Files.deleteIfExists(target);
                // Create notification for duplicate file attempt
                try {
                    notificationService.createNotification(
                            uploader.getId(),
                            "Duplicate File Upload",
                            "Your file was marked as REJECTED because it already exists in the database (detected by file hash).",
                            "DUPLICATE_FILE_REJECTED");
                } catch (Exception e) {
                    System.err.println("Failed to create notification: " + e.getMessage());
                }
                // Create paper record with REJECTED status
                Paper paper = new Paper();
                paper.setTitle(title);
                paper.setSubject(subject);
                paper.setUniversity(university);
                paper.setUploader(uploader);
                paper.setYear(year);
                paper.setExamType(examType);
                paper.setAuthor(author != null ? author : uploader.getFullName());
                paper.setStatus("REJECTED");
                paper.setFileUrl(null);
                paper.setFileHash(fileHash);
                Paper savedPaper = paperRepository.save(paper);

                Upload upload = new Upload();
                upload.setPaper(savedPaper);
                upload.setUploadedBy(uploader);
                upload.setOriginalFileName(file.getOriginalFilename());
                upload.setStoredPath(null);
                upload.setFileHash(fileHash);
                upload.setMimeType(file.getContentType());
                upload.setFileSize(file.getSize());
                upload.setUploadStatus("REJECTED");
                uploadRepository.save(upload);

                return toDto(savedPaper);
            }

            Paper paper = new Paper();
            paper.setTitle(title);
            paper.setSubject(subject);
            paper.setUniversity(university);
            paper.setUploader(uploader);
            paper.setYear(year);
            paper.setExamType(examType);
            paper.setAuthor(author != null ? author : uploader.getFullName());

            // Set status based on duplicate check and AI approval
            if (isDuplicate) {
                paper.setStatus("REJECTED");
            } else if (aiApproved) {
                paper.setStatus("APPROVED");
            } else {
                paper.setStatus("PENDING");
            }

            paper.setFileUrl("/files/" + fileName);
            paper.setFileHash(fileHash);
            Paper savedPaper = paperRepository.save(paper);

            Upload upload = new Upload();
            upload.setPaper(savedPaper);
            upload.setUploadedBy(uploader);
            upload.setOriginalFileName(file.getOriginalFilename());
            upload.setStoredPath(target.toString());
            upload.setFileHash(fileHash);
            upload.setMimeType(file.getContentType());
            upload.setFileSize(file.getSize());
            upload.setUploadStatus(isDuplicate ? "REJECTED" : "COMPLETED");
            uploadRepository.save(upload);

            return toDto(savedPaper);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store paper file", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to calculate file hash", e);
        }
    }

    public List<PaperDto> searchPapers(String query) {
        String q = query == null ? "" : query.trim();
        List<Paper> papers = paperRepository.findAll();

        if (q.isEmpty()) {
            return papers.stream()
                    .filter(paper -> "APPROVED".equalsIgnoreCase(paper.getStatus()))
                    .filter(paper -> paper.getFileUrl() != null && !paper.getFileUrl().isBlank())
                    .filter(paper -> uploadRepository.existsByPaper(paper))
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }

        String needle = q.toLowerCase();
        return papers.stream()
                .filter(paper -> "APPROVED".equalsIgnoreCase(paper.getStatus()))
                .filter(paper -> paper.getFileUrl() != null && !paper.getFileUrl().isBlank())
                .filter(paper -> uploadRepository.existsByPaper(paper))
                .filter(paper -> matchesSearch(paper, needle))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<PaperDto> getAllApprovedPapers() {
        return searchPapers("");
    }

    public List<PaperDto> getRecentPapers(int limit) {
        return paperRepository.findByStatusOrderByCreatedAtDesc("APPROVED", Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(paper -> paper.getFileUrl() != null && !paper.getFileUrl().isBlank())
                .filter(paper -> uploadRepository.existsByPaper(paper))
                .limit(limit)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public PaperDto getPaperById(Long id) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));
        if (paper.getFileUrl() == null || paper.getFileUrl().isBlank() || !uploadRepository.existsByPaper(paper)) {
            throw new IllegalArgumentException("Paper file is not available");
        }
        return toDto(paper);
    }

    public Double getAverageRating(Paper paper) {
        double avg = ratingRepository.findByPaper(paper).stream()
                .mapToDouble(r -> r.getScore())
                .average()
                .orElse(0.0);
        return avg == 0.0 ? 0.0 : Math.round(avg * 10.0) / 10.0;
    }

    private Paper createRejectedPaperRecord(MultipartFile file, String title, Subject subject, University university,
            User uploader, Integer year, String examType, String author, String fileHash) {
        Paper paper = new Paper();
        paper.setTitle(title);
        paper.setSubject(subject);
        paper.setUniversity(university);
        paper.setUploader(uploader);
        paper.setYear(year);
        paper.setExamType(examType);
        paper.setAuthor(author != null ? author : uploader.getFullName());
        paper.setStatus("REJECTED");
        paper.setFileUrl(null);
        paper.setFileHash(fileHash);
        Paper savedPaper = paperRepository.save(paper);

        Upload upload = new Upload();
        upload.setPaper(savedPaper);
        upload.setUploadedBy(uploader);
        upload.setOriginalFileName(file != null ? file.getOriginalFilename() : title);
        upload.setStoredPath(null);
        upload.setFileHash(fileHash);
        upload.setMimeType(file != null ? file.getContentType() : "application/octet-stream");
        upload.setFileSize(file != null ? file.getSize() : 0L);
        upload.setUploadStatus("REJECTED");
        uploadRepository.save(upload);
        return savedPaper;
    }

    private void createVerificationLog(Paper paper, Upload upload, String stage, Double score, String details) {
        VerificationLog log = new VerificationLog();
        log.setPaper(paper);
        log.setUpload(upload);
        log.setStage(stage);
        log.setScore(score);
        log.setDetailsJson(details);
        verificationLogRepository.save(log);
    }

    private String fileHashForRejectedUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        try {
            return calculateFileHash(file.getResource().getFile().toPath());
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isObviousSubjectMismatch(String title, String selectedSubject) {
        if (title == null || title.isBlank() || selectedSubject == null || selectedSubject.isBlank()) {
            return false;
        }

        String normalizedTitle = normalizeSubjectKey(title);
        String selectedKey = normalizeSubjectKey(selectedSubject);

        java.util.Map<String, java.util.List<String>> subjectKeywords = new java.util.HashMap<>();
        subjectKeywords.put("database management systems", java.util.List.of(
                "database", "dbms", "sql", "normalization", "transaction", "index", "query", "er model",
                "entity relationship", "schema"));
        subjectKeywords.put("operating systems", java.util.List.of(
                "operating system", "os", "process", "thread", "scheduler", "deadlock", "memory", "kernel"));
        subjectKeywords.put("computer networks", java.util.List.of(
                "network", "tcp", "ip", "routing", "protocol", "socket", "osi", "switch", "routing table"));
        subjectKeywords.put("data structures", java.util.List.of(
                "stack", "queue", "tree", "graph", "heap", "linked list", "hash map", "algorithm"));

        java.util.List<String> selectedTerms = subjectKeywords.getOrDefault(selectedKey,
                java.util.List.of(selectedKey));
        java.util.List<String> otherTerms = subjectKeywords.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(selectedKey))
                .flatMap(entry -> entry.getValue().stream())
                .toList();

        boolean selectedMatch = selectedTerms.stream().anyMatch(normalizedTitle::contains);
        boolean otherMatch = otherTerms.stream().anyMatch(normalizedTitle::contains);
        return otherMatch && !selectedMatch;
    }

    private String normalizeOptionalText(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    private Subject resolveSubject(String subjectName) {
        String normalized = normalizeOptionalText(subjectName, "General");
        String canonicalName = canonicalizeSubjectName(normalized);

        return subjectRepository.findByNameIgnoreCase(normalized)
                .or(() -> subjectRepository.findByCanonicalNameIgnoreCase(normalized))
                .or(() -> subjectRepository.findByName(canonicalName))
                .or(() -> subjectRepository.findByCanonicalName(canonicalName))
                .or(() -> subjectRepository.findByNameIgnoreCase(canonicalName))
                .or(() -> subjectRepository.findByCanonicalNameIgnoreCase(canonicalName))
                .or(() -> subjectAliasRepository.findByAliasIgnoreCase(normalized)
                        .map(SubjectAlias::getSubject))
                .or(() -> subjectAliasRepository.findByAliasIgnoreCase(canonicalName)
                        .map(SubjectAlias::getSubject))
                .or(() -> findMatchingSubjectByCanonicalizedName(normalized, canonicalName))
                .orElseGet(() -> {
                    Subject newSubject = new Subject();
                    newSubject.setName(canonicalName);
                    newSubject.setCanonicalName(canonicalName);
                    return subjectRepository.save(newSubject);
                });
    }

    private Optional<Subject> findMatchingSubjectByCanonicalizedName(String rawName, String canonicalName) {
        String rawKey = normalizeSubjectKey(rawName);
        String canonicalKey = normalizeSubjectKey(canonicalName);

        return subjectRepository.findAll().stream()
                .filter(subject -> matchesSubjectVariant(subject, rawKey, canonicalKey))
                .findFirst();
    }

    private boolean matchesSubjectVariant(Subject subject, String rawKey, String canonicalKey) {
        List<String> candidateKeys = new ArrayList<>();

        if (subject.getName() != null) {
            candidateKeys.add(normalizeSubjectKey(subject.getName()));
            candidateKeys.add(normalizeSubjectKey(canonicalizeSubjectName(subject.getName())));
        }
        if (subject.getCanonicalName() != null) {
            candidateKeys.add(normalizeSubjectKey(subject.getCanonicalName()));
            candidateKeys.add(normalizeSubjectKey(canonicalizeSubjectName(subject.getCanonicalName())));
        }
        if (subjectAliasRepository.findBySubject(subject) != null) {
            subjectAliasRepository.findBySubject(subject).forEach(alias -> {
                if (alias.getAlias() != null) {
                    candidateKeys.add(normalizeSubjectKey(alias.getAlias()));
                    candidateKeys.add(normalizeSubjectKey(canonicalizeSubjectName(alias.getAlias())));
                }
            });
        }

        return candidateKeys.stream().anyMatch(key -> key.equals(rawKey) || key.equals(canonicalKey));
    }

    private String normalizeSubjectKey(String value) {
        if (value == null || value.isBlank()) {
            return "general";
        }
        return value.trim().replaceAll("\\s+", " ").replaceAll("[^a-zA-Z0-9]+", " ")
                .trim().toLowerCase(Locale.ROOT);
    }

    private String canonicalizeSubjectName(String value) {
        if (value == null || value.isBlank()) {
            return "General";
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        String key = normalizeSubjectKey(normalized);

        if (key.equals("database management systems") || key.equals("database systems") || key.equals("dbms")) {
            return "Database Management Systems";
        }
        if (key.equals("operating systems") || key.equals("os") || key.equals("operating system")) {
            return "Operating Systems";
        }
        if (key.equals("computer networks") || key.equals("cn") || key.equals("computer network")) {
            return "Computer Networks";
        }
        if (key.equals("general")) {
            return "General";
        }
        return normalized;
    }

    private boolean matchesSearch(Paper paper, String query) {
        String title = paper.getTitle() == null ? "" : paper.getTitle().toLowerCase();
        String subject = paper.getSubject() != null && paper.getSubject().getCanonicalName() != null
                ? paper.getSubject().getCanonicalName().toLowerCase()
                : "";
        String university = paper.getUniversity() != null && paper.getUniversity().getName() != null
                ? paper.getUniversity().getName().toLowerCase()
                : "";
        String examType = paper.getExamType() == null ? "" : paper.getExamType().toLowerCase();
        String author = paper.getAuthor() == null ? "" : paper.getAuthor().toLowerCase();

        // Check if query matches any subject alias
        boolean matchesSubjectAlias = false;
        if (paper.getSubject() != null) {
            matchesSubjectAlias = subjectAliasRepository.findBySubject(paper.getSubject()).stream()
                    .anyMatch(alias -> alias.getAlias().toLowerCase().contains(query));
        }

        return title.contains(query)
                || subject.contains(query)
                || university.contains(query)
                || examType.contains(query)
                || author.contains(query)
                || matchesSubjectAlias;
    }

    private PaperDto toDto(Paper paper) {
        PaperDto dto = new PaperDto();
        dto.setId(paper.getId());
        dto.setTitle(paper.getTitle());
        dto.setSubjectName(paper.getSubject() != null ? paper.getSubject().getCanonicalName() : "Unknown");
        dto.setUniversityName(paper.getUniversity() != null ? paper.getUniversity().getName() : "Unknown");
        dto.setYear(paper.getYear());
        dto.setExamType(paper.getExamType());
        dto.setAuthor(paper.getAuthor());
        dto.setStatus(paper.getStatus());
        dto.setFileUrl(paper.getFileUrl());
        dto.setAverageRating(getAverageRating(paper));
        return dto;
    }

    private String calculateFileHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] hashBytes = digest.digest(fileBytes);
        return HexFormat.of().formatHex(hashBytes);
    }
}
