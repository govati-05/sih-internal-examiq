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

    public StudentDashboardService(UserRepository userRepository,
            PaperRepository paperRepository,
            BookmarkRepository bookmarkRepository,
            NotificationRepository notificationRepository,
            UploadRepository uploadRepository,
            RatingRepository ratingRepository,
            UniversityRepository universityRepository) {
        this.userRepository = userRepository;
        this.paperRepository = paperRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.notificationRepository = notificationRepository;
        this.uploadRepository = uploadRepository;
        this.ratingRepository = ratingRepository;
        this.universityRepository = universityRepository;
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
        return payload;
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
        return profile;
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
        userRepository.save(user);
        return getProfile(user);
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
        item.put("subjectName", paper.getSubject() != null ? paper.getSubject().getCanonicalName() : "Unknown");
        item.put("universityName", paper.getUniversity() != null ? paper.getUniversity().getName() : "Unknown");
        item.put("year", paper.getYear());
        item.put("examType", paper.getExamType());
        item.put("author", paper.getAuthor());
        item.put("status", paper.getStatus());
        item.put("fileUrl", paper.getFileUrl());
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
