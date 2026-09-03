package com.examiq.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "student_year")
    private Integer studentYear;

    @Column(length = 30)
    private String status = "PENDING";

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "score_percentage")
    private Double scorePercentage;

    @Lob
    @Column(name = "questions_json", columnDefinition = "LONGTEXT")
    private String questionsJson;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
