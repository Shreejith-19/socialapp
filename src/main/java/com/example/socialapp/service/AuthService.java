package com.example.socialapp.service;

import com.example.socialapp.dto.AuthRequest;
import com.example.socialapp.dto.AuthResponse;
import com.example.socialapp.dto.UserDTO;

/**
 * Authentication Service Interface.
 * Defines authentication-related operations.
 */
public interface AuthService {
    
    /**
     * Register a new user.
     */
    UserDTO register(UserDTO userDTO);
    
    /**
     * Authenticate user with credentials.
     */
    AuthResponse authenticate(AuthRequest authRequest);
    
    /**
     * Refresh JWT token.
     */
    AuthResponse refreshToken(String refreshToken);
    
    /**
     * Validate JWT token.
     */
    boolean validateToken(String token);
}
