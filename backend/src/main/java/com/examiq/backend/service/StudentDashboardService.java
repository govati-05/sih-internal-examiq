package com.examiq.backend.service;

import com.examiq.backend.entity.*;
import com.examiq.backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StudentDashboardService {

    private final UserRepository userRepository;
    private final PaperRepository paperRepository;
    private final BookmarkRepository bookmarkRepository;
    private final NotificationRepository notificationRepository;
    private final UploadRepository uploadRepository;
    private final RatingRepository ratingRepository;
    private final UniversityRepository universityRepository;
    private final ContributorScoreService contributorScoreService;
    private final RepeatedQuestionAnalysisService repeatedQuestionAnalysisService;
    private final QuizService quizService;

    public StudentDashboardService(UserRepository userRepository,
            PaperRepository paperRepository,
            BookmarkRepository bookmarkRepository,
            NotificationRepository notificationRepository,
            UploadRepository uploadRepository,
            RatingRepository ratingRepository,
            UniversityRepository universityRepository,
            ContributorScoreService contributorScoreService,
            RepeatedQuestionAnalysisService repeatedQuestionAnalysisService,
            QuizService quizService) {
        this.userRepository = userRepository;
        this.paperRepository = paperRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.notificationRepository = notificationRepository;
        this.uploadRepository = uploadRepository;
        this.ratingRepository = ratingRepository;
        this.universityRepository = universityRepository;
        this.contributorScoreService = contributorScoreService;
        this.repeatedQuestionAnalysisService = repeatedQuestionAnalysisService;
        this.quizService = quizService;
    }

    public User getAuthenticatedUser(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("User not authenticated");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public Map<String, Object> getDashboard(User user) {
        List<Paper> recentPapers = paperRepository.findAll().stream()
                .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getFileUrl() != null && !p.getFileUrl().isBlank())
                .filter(p -> uploadRepository.existsByPaper(p))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .toList();

        List<Bookmark> bookmarks = bookmarkRepository.findByUserOrderByCreatedAtDesc(user);
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);

        double topRating = paperRepository.findAll().stream()
                .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getFileUrl() != null && !p.getFileUrl().isBlank())
                .filter(p -> uploadRepository.existsByPaper(p))
                .map(this::averageRating)
                .filter(r -> r > 0)
                .max(Double::compare)
                .orElse(0.0);

        Map<String, Object> payload = new HashMap<>();
        payload.put("recentPapers", toPaperList(recentPapers));
        payload.put("notifications", toNotificationList(notifications));
        payload.put("stats", Map.of(
                "recentPapersCount", recentPapers.size(),
                "bookmarksCount", bookmarks.size(),
                "notificationsCount",
                (int) notifications.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count(),
                "topRatedValue", topRating == 0.0 ? 0.0 : Math.round(topRating * 10.0) / 10.0));
        payload.put("quickActions", List.of("Search", "Upload", "Bookmarks"));
        payload.put("recommendedResources", getRecommendedResources(user));
        payload.put("importantTopics", getImportantTopics(user));
        payload.put("preparationOverview", getPreparationOverview(user, bookmarks));
        return payload;
    }

    /**
     * Resources relevant to the student's own branch/year, drawn only from real
     * approved uploads - falls back to recent papers when there isn't a
     * branch/year match yet.
     */
    private List<Map<String, Object>> getRecommendedResources(User user) {
        List<Paper> approved = paperRepository.findAll().stream()
                .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getFileUrl() != null && !p.getFileUrl().isBlank())
                .filter(p -> uploadRepository.existsByPaper(p))
                .toList();

        List<Paper> matched = user.getYear() == null ? List.of()
                : approved.stream().filter(p -> user.getYear().equals(p.getStudentYear())).toList();

        List<Paper> source = matched.isEmpty() ? approved : matched;
        return source.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(6)
                .map(this::toPaperMap)
                .toList();
    }

    /**
     * High-recurrence topics pulled from the repeated-question analysis across
     * subjects the student has actually engaged with (bookmarks/uploads); falls
     * back to the subjects with the most approved papers otherwise.
     */
    private List<Map<String, Object>> getImportantTopics(User user) {
        List<Subject> subjectsOfInterest = new ArrayList<>();
        bookmarkRepository.findByUserOrderByCreatedAtDesc(user).forEach(b -> {
            if (b.getPaper() != null && b.getPaper().getSubject() != null
                    && !subjectsOfInterest.contains(b.getPaper().getSubject())) {
                subjectsOfInterest.add(b.getPaper().getSubject());
            }
        });
        uploadRepository.findByUploadedBy(user).forEach(u -> {
            if (u.getPaper() != null && u.getPaper().getSubject() != null
                    && !subjectsOfInterest.contains(u.getPaper().getSubject())) {
                subjectsOfInterest.add(u.getPaper().getSubject());
            }
        });

        if (subjectsOfInterest.isEmpty()) {
            paperRepository.findAll().stream()
                    .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()) && p.getSubject() != null)
                    .map(Paper::getSubject)
                    .distinct()
                    .limit(3)
                    .forEach(subjectsOfInterest::add);
        }

        List<Map<String, Object>> topics = new ArrayList<>();
        for (Subject subject : subjectsOfInterest) {
            List<Map<String, Object>> repeated = repeatedQuestionAnalysisService.getRepeatedQuestions(subject, 3);
            for (Map<String, Object> item : repeated) {
                Map<String, Object> withSubject = new HashMap<>(item);
                withSubject.put("subjectName", subject.getCanonicalName());
                topics.add(withSubject);
            }
        }
        return topics.stream().limit(8).toList();
    }

    private Map<String, Object> getPreparationOverview(User user, List<Bookmark> bookmarks) {
        List<Upload> uploads = uploadRepository.findByUploadedBy(user);
        Map<String, Object> overview = new HashMap<>();
        overview.put("bookmarksCount", bookmarks.size());
        overview.put("uploadsCount", uploads.size());
        overview.put("approvedUploadsCount",
                uploads.stream().filter(u -> u.getPaper() != null && "APPROVED".equalsIgnoreCase(u.getPaper().getStatus()))
                        .count());
        overview.put("branch", user.getBranch());
        overview.put("year", user.getYear());
        Map<String, Object> quizStats = quizService.getQuizStats(user);
        overview.put("quizAttemptsCount", quizStats.get("attemptsCount"));
        overview.put("averageQuizScore", quizStats.get("averageScore"));
        return overview;
    }

    public Map<String, Object> getProfile(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
        profile.put("role", user.getRole() != null ? user.getRole().getName() : null);
        profile.put("university", user.getUniversity() != null ? user.getUniversity().getName() : null);
        profile.put("status", user.getStatus());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("profilePictureUrl", user.getProfilePictureUrl());
        profile.put("branch", user.getBranch());
        profile.put("year", user.getYear());
        profile.put("section", user.getSection());
        profile.put("bio", user.getBio());
        profile.put("contributorScore", contributorScoreSummary(user));
        return profile;
    }

    private Map<String, Object> contributorScoreSummary(User user) {
        var score = contributorScoreService.getOrCreateScore(user.getId());
        List<Upload> uploads = uploadRepository.findByUploadedBy(user);
        long approvedUploads = uploads.stream()
                .filter(u -> u.getPaper() != null && "APPROVED".equalsIgnoreCase(u.getPaper().getStatus()))
                .count();
        double avgRatingReceived = uploads.stream()
                .map(Upload::getPaper)
                .filter(p -> p != null)
                .distinct()
                .mapToDouble(this::averageRating)
                .filter(r -> r > 0)
                .average()
                .orElse(0.0);

        Map<String, Object> summary = new HashMap<>();
        summary.put("points", score.getScore());
        summary.put("tier", score.getTier());
        summary.put("badge", contributorScoreService.badgeLabel(score.getTier()));
        summary.put("uploadedResourcesCount", uploads.size());
        summary.put("approvedResourcesCount", approvedUploads);
        summary.put("averageRatingReceived", Math.round(avgRatingReceived * 10.0) / 10.0);
        return summary;
    }

    public Map<String, Object> updateProfile(User user, Map<String, String> payload) {
        if (payload == null) {
            return getProfile(user);
        }

        if (payload.containsKey("fullName") && payload.get("fullName") != null && !payload.get("fullName").isBlank()) {
            user.setFullName(payload.get("fullName"));
        }
        if (payload.containsKey("email") && payload.get("email") != null && !payload.get("email").isBlank()) {
            user.setEmail(payload.get("email"));
        }
        if (payload.containsKey("university") && payload.get("university") != null
                && !payload.get("university").isBlank()) {
            String universityName = payload.get("university").trim();
            University university = universityRepository.findByNameIgnoreCase(universityName)
                    .orElseGet(() -> {
                        University newUniversity = new University();
                        newUniversity.setName(universityName);
                        return universityRepository.save(newUniversity);
                    });
            user.setUniversity(university);
        }
        if (payload.containsKey("branch")) {
            user.setBranch(blankToNull(payload.get("branch")));
        }
        if (payload.containsKey("section")) {
            user.setSection(blankToNull(payload.get("section")));
        }
        if (payload.containsKey("bio")) {
            user.setBio(blankToNull(payload.get("bio")));
        }
        if (payload.containsKey("year")) {
            String yearValue = payload.get("year");
            if (yearValue == null || yearValue.isBlank()) {
                user.setYear(null);
            } else {
                try {
                    int parsed = Integer.parseInt(yearValue.trim());
                    if (parsed < 1 || parsed > 4) {
                        throw new IllegalArgumentException("Year must be between 1 and 4");
                    }
                    user.setYear(parsed);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Year must be a valid number");
                }
            }
        }
        userRepository.save(user);
        return getProfile(user);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public List<Map<String, Object>> getBookmarks(User user) {
        return bookmarkRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(bookmark -> toPaperMap(bookmark.getPaper()))
                .toList();
    }

    public Map<String, Object> addBookmark(User user, Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        boolean exists = bookmarkRepository.existsByUserAndPaper(user, paper);
        if (exists) {
            throw new IllegalArgumentException("Paper already bookmarked");
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setPaper(paper);
        bookmarkRepository.save(bookmark);

        Map<String, Object> payload = new HashMap<>();
        payload.put("bookmarked", true);
        payload.put("paperId", paper.getId());
        payload.put("message", "Bookmark added");
        return payload;
    }

    public Map<String, Object> removeBookmark(User user, Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));
        Optional<Bookmark> bookmark = bookmarkRepository.findByUserAndPaper(user, paper);
        if (bookmark.isPresent()) {
            bookmarkRepository.delete(bookmark.get());
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookmarked", false);
        payload.put("paperId", paper.getId());
        payload.put("message", "Bookmark removed");
        return payload;
    }

    public List<Map<String, Object>> getMyUploads(User user) {
        List<Upload> uploads = uploadRepository.findByUploadedBy(user);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Upload upload : uploads) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", upload.getId());
            item.put("paperId", upload.getPaper() != null ? upload.getPaper().getId() : null);
            item.put("paperTitle",
                    upload.getPaper() != null ? upload.getPaper().getTitle() : upload.getOriginalFileName());
            item.put("status", upload.getUploadStatus());
            item.put("fileName", upload.getOriginalFileName());
            item.put("createdAt", upload.getCreatedAt());
            item.put("accessType", upload.getPaper() != null && upload.getPaper().getAccessType() != null
                    ? upload.getPaper().getAccessType() : "PUBLIC");
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(notification -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", notification.getId());
                    item.put("title", notification.getTitle());
                    item.put("message", notification.getMessage());
                    item.put("type", notification.getType());
                    item.put("isRead", Boolean.TRUE.equals(notification.getIsRead()));
                    item.put("createdAt", notification.getCreatedAt());
                    return item;
                })
                .toList();
    }

    public Map<String, Object> markNotificationRead(User user, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUser(notificationId, user)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", notification.getId());
        payload.put("isRead", true);
        return payload;
    }

    private List<Map<String, Object>> toPaperList(List<Paper> papers) {
        return papers.stream().map(this::toPaperMap).toList();
    }

    private List<Map<String, Object>> toNotificationList(List<Notification> notifications) {
        return notifications.stream().map(notification -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", notification.getId());
            item.put("title", notification.getTitle());
            item.put("message", notification.getMessage());
            item.put("type", notification.getType());
            item.put("isRead", Boolean.TRUE.equals(notification.getIsRead()));
            item.put("createdAt", notification.getCreatedAt());
            return item;
        }).toList();
    }

    private Map<String, Object> toPaperMap(Paper paper) {
        Map<String, Object> item = new HashMap<>();
        if (paper == null) {
            return item;
        }
        item.put("id", paper.getId());
        item.put("title", paper.getTitle());
        item.put("subjectId", paper.getSubject() != null ? paper.getSubject().getId() : null);
        item.put("subjectName", paper.getSubject() != null ? paper.getSubject().getCanonicalName() : "Unknown");
        item.put("universityName", paper.getUniversity() != null ? paper.getUniversity().getName() : "Unknown");
        item.put("year", paper.getYear());
        item.put("studentYear", paper.getStudentYear());
        item.put("examType", paper.getExamType());
        item.put("author", paper.getAuthor());
        item.put("status", paper.getStatus());
        item.put("fileUrl", paper.getFileUrl());
        item.put("accessType", paper.getAccessType() != null ? paper.getAccessType() : "PUBLIC");
        item.put("averageRating", averageRating(paper));
        return item;
    }

    private double averageRating(Paper paper) {
        return ratingRepository.findByPaper(paper).stream()
                .mapToDouble(Rating::getScore)
                .average()
                .orElse(0.0);
    }
}
