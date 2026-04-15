package com.example.socialapp.service.impl;

import com.example.socialapp.dto.ReportDTO;
import com.example.socialapp.entity.Post;
import com.example.socialapp.entity.Report;
import com.example.socialapp.entity.User;
import com.example.socialapp.exception.ConflictException;
import com.example.socialapp.exception.ResourceNotFoundException;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.ReportRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Report Service Implementation.
 * Provides report management logic for content moderation workflow.
 */
@Slf4j
@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public ReportServiceImpl(ReportRepository reportRepository, PostRepository postRepository,
                           UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ReportDTO submitReport(UUID postId, UUID reporterId, String reason) {
        log.info("User {} submitting report for post: {}", reporterId, postId);

        // Verify post exists
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId.toString()));

        // Verify reporter exists
        User reporter = userRepository.findById(reporterId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", reporterId.toString()));

        // Check for duplicate reports (same user, same post)
        if (reportRepository.existsByPostIdAndReporterId(postId, reporterId)) {
            log.warn("User {} has already reported post {}", reporterId, postId);
            throw new ConflictException("You have already reported this post");
        }

        // Create report
        Report report = Report.builder()
            .post(post)
            .reporter(reporter)
            .reason(reason)
            .build();

        Report savedReport = reportRepository.save(report);
        log.info("Report submitted: {} for post: {} by user: {}", savedReport.getId(), postId, reporterId);

        // Log report count for this post
        long reportCount = reportRepository.countByPostId(postId);
        log.info("Post {} now has {} reports", postId, reportCount);

        return mapToDTO(savedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDTO getReportById(UUID reportId) {
        log.debug("Fetching report: {}", reportId);
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId.toString()));
        return mapToDTO(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportDTO> getReportsByPostId(UUID postId) {
        log.debug("Fetching reports for post: {}", postId);
        return reportRepository.findByPostId(postId).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportDTO> getReportsByPostId(UUID postId, Pageable pageable) {
        log.debug("Fetching reports for post {} with pagination", postId);
        return reportRepository.findByPostId(postId, pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportDTO> getReportsByReporterId(UUID reporterId) {
        log.debug("Fetching reports by reporter: {}", reporterId);
        return reportRepository.findByReporterId(reporterId).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportDTO> getAllReports() {
        log.info("Fetching all reports (moderator review queue)");
        return reportRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportDTO> getAllReports(Pageable pageable) {
        log.info("Fetching all reports with pagination (moderator review queue)");
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public long getReportCountForPost(UUID postId) {
        return reportRepository.countByPostId(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserReportedPost(UUID postId, UUID reporterId) {
        return reportRepository.existsByPostIdAndReporterId(postId, reporterId);
    }

    @Override
    public void deleteReport(UUID reportId) {
        log.info("Deleting report: {}", reportId);
        if (!reportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Report", "id", reportId.toString());
        }
        reportRepository.deleteById(reportId);
        log.info("Report deleted: {}", reportId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalReportCount() {
        return reportRepository.count();
    }

    /**
     * Map Report entity to ReportDTO.
     */
    private ReportDTO mapToDTO(Report report) {
        return ReportDTO.builder()
            .id(report.getId())
            .postId(report.getPost().getId())
            .postContent(report.getPost().getContent())
            .reporterId(report.getReporter().getId())
            .reporterUsername(report.getReporter().getUsername())
            .reason(report.getReason())
            .createdAt(report.getCreatedAt())
            .build();
    }
}
