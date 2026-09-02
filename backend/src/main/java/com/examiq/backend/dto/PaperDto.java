package com.examiq.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class PaperDto {
    private Long id;
    private String title;
    private Long subjectId;
    private String subjectName;
    private String universityName;
    private Integer year;
    private Integer studentYear;
    private String examType;
    private String author;
    private String status;
    private String fileUrl;
    private Double averageRating;
    private String accessType;
    private Long viewCount;
    private Long downloadCount;
    private Long uploaderId;
    private String uploaderName;
    private List<String> warnings;
}
