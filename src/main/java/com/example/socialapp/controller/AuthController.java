package com.example.socialapp.controller;

import com.example.socialapp.dto.AuthRequest;
import com.example.socialapp.dto.AuthResponse;
import com.example.socialapp.dto.UserDTO;
import com.example.socialapp.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * Authentication Controller.
 * Handles authentication-related HTTP requests.
 */
@Slf4j
@RestController
@RequestMapping("/v1/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * User registration endpoint.
     */
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody UserDTO userDTO) {
        log.info("Registration request for email: {}", userDTO.getEmail());
        UserDTO registeredUser = authService.register(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }

    /**
     * User signup endpoint (alias for register).
     */
    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signup(@Valid @RequestBody UserDTO userDTO) {
        log.info("Signup request for email: {}", userDTO.getEmail());
        UserDTO registeredUser = authService.register(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }

    /**
     * User login endpoint.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        log.info("Login request for email: {}", authRequest.getEmail());
        AuthResponse authResponse = authService.authenticate(authRequest);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Token refresh endpoint.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String bearerToken) {
        log.info("Token refresh request");
        String token = bearerToken.replace("Bearer ", "");
        AuthResponse authResponse = authService.refreshToken(token);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Token validation endpoint.
     */
    @PostMapping("/validate-token")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String bearerToken) {
        log.info("Token validation request");
        String token = bearerToken.replace("Bearer ", "");
        boolean isValid = authService.validateToken(token);
        return ResponseEntity.ok(isValid);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Authentication service is running");
    }
}
