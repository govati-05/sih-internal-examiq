package com.examiq.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "papers")
@Getter
@Setter
@NoArgsConstructor
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(name = "paper_year")
    private Integer year;

    @Column(name = "exam_type")
    private String examType;

    @Column
    private String author;

    @Column(length = 30)
    private String status = "PENDING";

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "ocr_confidence")
    private Double ocrConfidence;

    @Column(name = "duplicate_score")
    private Double duplicateScore;

    @Column(name = "ai_confidence_score")
    private Double aiConfidenceScore;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(columnDefinition = "TEXT")
    private String ocrText;

    @Column(name = "metadata_json")
    private String metadataJson;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
