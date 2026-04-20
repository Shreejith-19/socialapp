package com.example.socialapp.controller;

import com.example.socialapp.dto.ModerationLogDTO;
import com.example.socialapp.dto.UserDTO;
import com.example.socialapp.entity.ModerationLog;
import com.example.socialapp.entity.User;
import com.example.socialapp.repository.ModerationLogRepository;
import com.example.socialapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Log Controller.
 * Handles admin endpoints for viewing audit logs, moderation records, and user management.
 * 
 * ADMIN Role Endpoints:
 * - GET /api/v1/admin/logs - Get all moderation logs sorted by latest
 * - GET /api/v1/admin/users - Get all users with pagination
 * 
 * All endpoints in this controller require ADMIN role.
 */
@Slf4j
@RestController
@RequestMapping("/v1/admin")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private final ModerationLogRepository moderationLogRepository;
    private final UserRepository userRepository;

    public AdminLogController(ModerationLogRepository moderationLogRepository, UserRepository userRepository) {
        this.moderationLogRepository = moderationLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all moderation logs sorted by latest first.
     * Returns a list of all moderation actions taken by moderators.
     * Requires ADMIN role.
     * 
     * @return ResponseEntity containing list of ModerationLogDTO
     */
    @GetMapping("/logs")
    public ResponseEntity<List<ModerationLogDTO>> getAllModerationLogs() {
        log.info("Fetching all moderation logs");

        // Fetch all logs sorted by creation date (newest first)
        List<ModerationLog> logs = moderationLogRepository.findAllByOrderByCreatedAtDesc();

        // Convert to DTO
        List<ModerationLogDTO> logDTOs = logs.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        log.info("Returned {} moderation logs", logDTOs.size());
        return ResponseEntity.ok(logDTOs);
    }

    /**
     * Get all users with pagination.
     * Supports pagination for displaying users in admin panel.
     * Requires ADMIN role.
     * 
     * @param page Zero-based page number (default: 0)
     * @param size Page size (default: 20)
     * @return ResponseEntity containing paginated list of UserDTO
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Fetching users with pagination - page: {}, size: {}", page, size);
        
        // Create pageable object
        Pageable pageable = PageRequest.of(page, size);
        
        // Fetch paginated users
        Page<User> usersPage = userRepository.findAll(pageable);
        
        // Convert to DTO
        Page<UserDTO> userDTOsPage = usersPage.map(this::mapUserToDTO);
        
        log.info("Returned {} users from page {}", userDTOsPage.getNumberOfElements(), page);
        return ResponseEntity.ok(userDTOsPage);
    }

    /**
     * Map User entity to UserDTO.
     */
    private ModerationLogDTO mapToDTO(ModerationLog log) {
        return ModerationLogDTO.builder()
            .id(log.getId())
            .moderatorId(log.getModeratorId())
            .moderatorName(log.getModeratorName())
            .action(log.getAction())
            .postId(log.getPostId())
            .createdAt(log.getCreatedAt())
            .build();
    }
    /**
     * Map User entity to UserDTO.
     */
    private UserDTO mapUserToDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .enabled(user.getEnabled())
            .status(user.getStatus() != null ? user.getStatus().name() : "ACTIVE")
            .isBanned(user.getStatus() != null && (user.getStatus().name().equals("TEMP_BANNED") || user.getStatus().name().equals("PERM_BANNED")))
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .roles(user.getRoles() != null ? user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toSet()) : java.util.Set.of())
            .build();
    }}
