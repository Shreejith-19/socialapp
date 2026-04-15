package com.example.socialapp.controller;

import com.example.socialapp.dto.UserDTO;
import com.example.socialapp.service.UserService;
import com.example.socialapp.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * User Controller.
 * Handles user management HTTP requests with role-based access control.
 * 
 * Endpoints:
 * - GET /me - Any authenticated user can view their own profile
 * - GET /{id} - Admin only: view any user profile
 * - PUT /me - Any authenticated user can update their own profile
 * - PUT /{id} - Admin only: update any user profile
 * - DELETE /{id} - Admin only: delete any user
 * - GET /exists/{email} - Public: check if email exists (for registration)
 */
@Slf4j
@RestController
@RequestMapping("/v1/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get current logged-in user information.
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<UserDTO> getCurrentUser() {
        log.info("Fetching current user information");
        String email = SecurityUtil.getCurrentUserEmail();
        UserDTO userDTO = userService.getUserByEmail(email);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * Get user by ID.
     * Admin can view any user. Regular users can only view their own info (authorization handled at controller level).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        log.info("Fetching user with ID: {}", id);
        UserDTO userDTO = userService.getUserById(id);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * Update current user information.
     */
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<UserDTO> updateCurrentUser(@Valid @RequestBody UserDTO userDTO) {
        log.info("Updating current user information");
        String email = SecurityUtil.getCurrentUserEmail();
        UserDTO currentUser = userService.getUserByEmail(email);
        UserDTO updatedUser = userService.updateUser(currentUser.getId(), userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Update user (Admin only).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @Valid @RequestBody UserDTO userDTO) {
        log.info("Admin updating user with ID: {}", id);
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Delete user (Admin only).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        log.info("Admin deleting user with ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if user exists by email.
     */
    @GetMapping("/exists/{email}")
    public ResponseEntity<Boolean> userExists(@PathVariable @NotBlank String email) {
        log.info("Checking if user exists with email: {}", email);
        boolean exists = userService.userExists(email);
        return ResponseEntity.ok(exists);
    }
}
