package com.example.socialapp.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for ban appeal submission.
 * Contains the ban ID and appeal reason.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppealRequest {
    
    @NotNull(message = "Ban ID is required")
    private UUID banId;
    
    @NotBlank(message = "Reason is required")
    private String reason;
}
