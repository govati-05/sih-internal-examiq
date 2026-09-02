package com.examiq.backend.dto;

import lombok.Data;

@Data
public class PaperDto {
    private Long id;
    private String title;
    private String subjectName;
    private String universityName;
    private Integer year;
    private String examType;
    private String author;
    private String status;
    private String fileUrl;
    private Double averageRating;
}
