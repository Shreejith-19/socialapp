package com.example.socialapp.service;

import com.example.socialapp.dto.UserDTO;
import com.example.socialapp.entity.Penalty;
import com.example.socialapp.enums.PenaltyType;
import com.example.socialapp.enums.UserStatus;
import java.util.UUID;

/**
 * User Service Interface.
 * Defines user management operations.
 */
public interface UserService {
    
    /**
     * Get user by ID.
     */
    UserDTO getUserById(UUID id);
    
    /**
     * Get user by email.
     */
    UserDTO getUserByEmail(String email);
    
    /**
     * Update user information.
     */
    UserDTO updateUser(UUID id, UserDTO userDTO);
    
    /**
     * Delete user.
     */
    void deleteUser(UUID id);
    
    /**
     * Check if user exists by email.
     */
    boolean userExists(String email);

    /**
     * Issue a penalty to a user.
     * Automatically updates user status based on penalty type.
     *
     * @param userId the user ID
     * @param penaltyType the type of penalty
     * @param points the penalty points
     * @param reason the reason for the penalty
     * @return the created Penalty
     */
    Penalty receivePenalty(UUID userId, PenaltyType penaltyType, Integer points, String reason);

    /**
     * Update user status.
     * Checks for active bans to determine if posting is allowed.
     *
     * @param userId the user ID
     * @param status the new status
     */
    void updateStatus(UUID userId, UserStatus status);
}
