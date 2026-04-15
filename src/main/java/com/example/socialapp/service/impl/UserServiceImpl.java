package com.example.socialapp.service.impl;

import com.example.socialapp.dto.UserDTO;
import com.example.socialapp.entity.Ban;
import com.example.socialapp.entity.Penalty;
import com.example.socialapp.entity.User;
import com.example.socialapp.enums.BanType;
import com.example.socialapp.enums.PenaltyType;
import com.example.socialapp.enums.UserStatus;
import com.example.socialapp.exception.ResourceNotFoundException;
import com.example.socialapp.repository.BanRepository;
import com.example.socialapp.repository.PenaltyRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User Service Implementation.
 * Provides user management logic.
 */
@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PenaltyRepository penaltyRepository;
    private final BanRepository banRepository;

    public UserServiceImpl(UserRepository userRepository, PenaltyRepository penaltyRepository,
                          BanRepository banRepository) {
        this.userRepository = userRepository;
        this.penaltyRepository = penaltyRepository;
        this.banRepository = banRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(UUID id) {
        log.info("Fetching user with ID: {}", id);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));
        return mapToDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        log.info("Fetching user with email: {}", email);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToDTO(user);
    }

    @Override
    public UserDTO updateUser(UUID id, UserDTO userDTO) {
        log.info("Updating user with ID: {}", id);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));

        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", id);
        return mapToDTO(updatedUser);
    }

    @Override
    public void deleteUser(UUID id) {
        log.info("Deleting user with ID: {}", id);
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id.toString());
        }
        userRepository.deleteById(id);
        log.info("User deleted successfully: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Penalty receivePenalty(UUID userId, PenaltyType penaltyType, Integer points, String reason) {
        log.info("User {} received penalty: type={}, points={}", userId, penaltyType, points);

        // Load user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        // Create penalty record
        Penalty penalty = Penalty.builder()
            .user(user)
            .type(penaltyType)
            .points(points)
            .reason(reason)
            .build();

        Penalty savedPenalty = penaltyRepository.save(penalty);
        log.info("Penalty saved: {}", savedPenalty.getId());

        // Update user status based on penalty type
        switch (penaltyType) {
            case WARNING:
                log.info("Warning issued to user: {}", userId);
                // Just record the warning, no status change
                break;

            case TEMP_BAN:
                log.warn("User temporarily banned: {}", userId);
                user.setStatus(UserStatus.TEMP_BANNED);
                // Create temporary ban record (7 days)
                Ban tempBan = Ban.builder()
                    .user(user)
                    .type(BanType.TEMPORARY)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusDays(7))
                    .reason(reason)
                    .build();
                banRepository.save(tempBan);
                userRepository.save(user);
                break;

            case PERM_BAN:
                log.warn("User permanently banned: {}", userId);
                user.setStatus(UserStatus.PERM_BANNED);
                // Create permanent ban record
                Ban permBan = Ban.builder()
                    .user(user)
                    .type(BanType.PERMANENT)
                    .startDate(LocalDateTime.now())
                    .reason(reason)
                    .build();
                banRepository.save(permBan);
                userRepository.save(user);
                break;
        }

        return savedPenalty;
    }

    @Override
    public void updateStatus(UUID userId, UserStatus status) {
        log.info("Updating user status: userId={}, status={}", userId, status);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        // Check for active bans before allowing status change to ACTIVE
        if (UserStatus.ACTIVE.equals(status)) {
            List<Ban> activeBans = banRepository.findActiveBansByUser(user);
            if (!activeBans.isEmpty()) {
                log.warn("Cannot activate user with active bans: {}", userId);
                throw new IllegalStateException("User has active bans and cannot be activated");
            }
        }

        user.setStatus(status);
        userRepository.save(user);
        log.info("User status updated: {}", userId);
    }

    /**
     * Map User entity to UserDTO.
     */
    private UserDTO mapToDTO(User user) {
        UserDTO.UserDTOBuilder builder = UserDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .enabled(user.getEnabled())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .status(user.getStatus().toString())
            .isBanned(user.isBanned())
            .roles(user.getRoles().stream()
                .map(role -> role.getName().toString())
                .collect(java.util.stream.Collectors.toSet()));

        // Fetch and populate ban information if user is banned
        if (user.isBanned()) {
            banRepository.findLatestActiveBanByUser(user).ifPresentOrElse(
                ban -> {
                    builder.remainingBanDays(ban.getRemainingDays());
                    builder.remainingBanHours(ban.getRemainingHours());
                    String message = "You are temporarily banned. " + ban.getFormattedRemainingTime();
                    if (ban.isPermanent()) {
                        message = "Your account has been permanently banned.";
                    }
                    builder.banMessage(message);
                    log.info("User {} ban info: {}", user.getId(), message);
                },
                () -> {
                    builder.remainingBanDays(0L);
                    builder.remainingBanHours(0L);
                    builder.banMessage("");
                }
            );
        }

        return builder.build();
    }
}
