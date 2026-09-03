package com.examiq.backend.service;

import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Report;
import com.examiq.backend.entity.User;
import com.examiq.backend.repository.PaperRepository;
import com.examiq.backend.repository.ReportRepository;
import com.examiq.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;

    public ReportService(ReportRepository reportRepository, PaperRepository paperRepository, UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.paperRepository = paperRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Report reportPaper(Long paperId, Long userId, String reportType, String description) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Report report = new Report();
        report.setPaper(paper);
        report.setUser(user);
        report.setReportType(reportType);
        report.setDescription(description);
        report.setStatus("OPEN");
        return reportRepository.save(report);
    }

    public List<Report> getPaperReports(Long paperId, User requestingUser) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));
        boolean isOwner = paper.getUploader() != null
                && paper.getUploader().getId().equals(requestingUser.getId());
        boolean isAdmin = requestingUser.getRole() != null
                && "ADMIN".equalsIgnoreCase(requestingUser.getRole().getName());
        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("You are not authorized to view reports for this resource");
        }
        return reportRepository.findByPaperOrderByCreatedAtDesc(paper);
    }

    public List<Report> getOpenReports() {
        return reportRepository.findByStatusOrderByCreatedAtDesc("OPEN");
    }

    @Transactional
    public Report updateReportStatus(Long reportId, String status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        report.setStatus(status);
        return reportRepository.save(report);
    }
}
