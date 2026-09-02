package com.examiq.backend.controller;

import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Upload;
import com.examiq.backend.entity.User;
import com.examiq.backend.repository.PaperRepository;
import com.examiq.backend.repository.UploadRepository;
import com.examiq.backend.security.AuthenticatedUserResolver;
import com.examiq.backend.service.AccessRequestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Serves stored resource files with server-side permission enforcement.
 * Direct static hosting was removed on purpose: a resource's access type and
 * an approver's decision on an access request are the only things that may
 * unlock the bytes, and that logic cannot live in a static resource handler.
 */
@RestController
@RequestMapping("/api")
public class FileAccessController {

    private static final Set<String> DOWNLOAD_CAPABLE = Set.of("VIEW_DOWNLOAD");

    private final PaperRepository paperRepository;
    private final UploadRepository uploadRepository;
    private final AccessRequestService accessRequestService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Value("${app.storage.path:./storage}")
    private String storagePath;

    public FileAccessController(PaperRepository paperRepository,
            UploadRepository uploadRepository,
            AccessRequestService accessRequestService,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.paperRepository = paperRepository;
        this.uploadRepository = uploadRepository;
        this.accessRequestService = accessRequestService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/papers/{id}/file")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<Resource> getFile(@PathVariable Long id,
            @RequestParam(value = "mode", defaultValue = "view") String mode) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        if (!"APPROVED".equalsIgnoreCase(paper.getStatus())) {
            throw new IllegalArgumentException("This resource is not available");
        }

        User user = authenticatedUserResolver.getCurrentUser();
        String accessLevel = accessRequestService.resolveAccessLevel(paper, user);
        if ("NONE".equals(accessLevel)) {
            throw new AccessDeniedForResourceException("You do not have access to this resource. Request access first.");
        }

        boolean wantsDownload = "download".equalsIgnoreCase(mode);
        if (wantsDownload && !DOWNLOAD_CAPABLE.contains(accessLevel)) {
            throw new AccessDeniedForResourceException("You only have view access to this resource.");
        }

        Upload upload = uploadRepository.findByPaper(paper)
                .orElseThrow(() -> new IllegalArgumentException("File not found for this resource"));
        String storedPath = upload.getStoredPath();
        if (storedPath == null || storedPath.isBlank()) {
            throw new IllegalArgumentException("File not found for this resource");
        }

        Path basePath = Paths.get(storagePath).toAbsolutePath().normalize();
        Path filePath = Paths.get(storedPath).toAbsolutePath().normalize();
        if (!filePath.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        File file = filePath.toFile();
        if (!file.exists()) {
            throw new IllegalArgumentException("File no longer exists on the server");
        }

        if (wantsDownload) {
            paper.setDownloadCount((paper.getDownloadCount() == null ? 0L : paper.getDownloadCount()) + 1);
            paperRepository.save(paper);
        }

        Resource resource = new FileSystemResource(file);
        MediaType mediaType = upload.getMimeType() != null
                ? MediaType.parseMediaType(upload.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;
        String disposition = (wantsDownload ? "attachment" : "inline") + "; filename=\""
                + sanitizeFileName(upload.getOriginalFileName()) + "\"";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "resource";
        }
        return name.replaceAll("[\\r\\n\"]", "_");
    }

    public static class AccessDeniedForResourceException extends RuntimeException {
        public AccessDeniedForResourceException(String message) {
            super(message);
        }
    }
}
