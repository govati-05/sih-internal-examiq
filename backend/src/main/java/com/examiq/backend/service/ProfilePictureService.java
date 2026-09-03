package com.examiq.backend.service;

import com.examiq.backend.entity.User;
import com.examiq.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

@Service
public class ProfilePictureService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final UserRepository userRepository;

    @Value("${app.storage.path:./storage}")
    private String storagePath;

    public ProfilePictureService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, String> upload(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A profile picture file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPG, PNG, or WEBP images are allowed");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Profile picture must be smaller than 5MB");
        }

        deleteExistingFile(user);

        String extension = switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String fileName = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + extension;

        try {
            Path avatarsDir = Paths.get(storagePath, "avatars").toAbsolutePath().normalize();
            Files.createDirectories(avatarsDir);
            Path target = avatarsDir.resolve(fileName);
            Files.copy(file.getInputStream(), target);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store profile picture", e);
        }

        user.setProfilePictureUrl("/avatars/" + fileName);
        userRepository.save(user);
        return Map.of("profilePictureUrl", user.getProfilePictureUrl());
    }

    @Transactional
    public void remove(User user) {
        deleteExistingFile(user);
        user.setProfilePictureUrl(null);
        userRepository.save(user);
    }

    private void deleteExistingFile(User user) {
        if (user.getProfilePictureUrl() == null || user.getProfilePictureUrl().isBlank()) {
            return;
        }
        try {
            String fileName = user.getProfilePictureUrl().replaceFirst("^/avatars/", "");
            Path existing = Paths.get(storagePath, "avatars", fileName).toAbsolutePath().normalize();
            Path avatarsDir = Paths.get(storagePath, "avatars").toAbsolutePath().normalize();
            if (existing.startsWith(avatarsDir)) {
                Files.deleteIfExists(existing);
            }
        } catch (IOException e) {
            System.err.println("Failed to delete old profile picture: " + e.getMessage());
        }
    }
}
