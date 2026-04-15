package com.example.socialapp.frontend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Authentication Response.
 * Matches backend AuthResponse structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserDTO user;
    
    /**
     * Convenience method to get the access token (primary token for requests).
     */
    public String getToken() {
        return accessToken;
    }
}
