package com.example.socialapp.service.impl;

import com.example.socialapp.dto.AuthRequest;
import com.example.socialapp.dto.AuthResponse;
import com.example.socialapp.dto.UserDTO;
import com.example.socialapp.entity.Role;
import com.example.socialapp.entity.User;
import com.example.socialapp.exception.AuthenticationException;
import com.example.socialapp.exception.ConflictException;
import com.example.socialapp.repository.RoleRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.security.JwtTokenProvider;
import com.example.socialapp.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;

/**
 * Authentication Service Implementation.
 * Provides authentication and registration logic.
 */
@Slf4j
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                         RoleRepository roleRepository,
                         PasswordEncoder passwordEncoder,
                         AuthenticationManager authenticationManager,
                         JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public UserDTO register(UserDTO userDTO) {
        log.info("Registering new user with email: {}", userDTO.getEmail());

        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new ConflictException("Email already exists", "email", userDTO.getEmail());
        }

        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        // Generate username from email (part before @)
        user.setUsername(userDTO.getEmail().split("@")[0]);
        user.setEnabled(true);

        // Assign default USER role
        Role userRole = roleRepository.findByName(Role.RoleType.USER)
            .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.setRoles(Collections.singleton(userRole));

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with email: {}", savedUser.getEmail());

        return mapToDTO(savedUser);
    }

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        log.info("Authenticating user with email: {}", authRequest.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    authRequest.getEmail(),
                    authRequest.getPassword()
                )
            );

            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authRequest.getEmail());

            User user = userRepository.findByEmail(authRequest.getEmail())
                .orElseThrow(() -> new AuthenticationException("User not found after successful authentication"));

            UserDTO userDTO = mapToDTO(user);

            log.info("User authenticated successfully: {}", authRequest.getEmail());
            return new AuthResponse(accessToken, refreshToken, userDTO);

        } catch (org.springframework.security.core.AuthenticationException ex) {
            log.error("Authentication failed for user: {}", authRequest.getEmail());
            throw new com.example.socialapp.exception.AuthenticationException("Invalid email or password", ex);
        }
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing JWT token");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new com.example.socialapp.exception.AuthenticationException("Invalid or expired refresh token");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        String newAccessToken = jwtTokenProvider.generateAccessToken(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthenticationException("User not found"));

        UserDTO userDTO = mapToDTO(user);
        return new AuthResponse(newAccessToken, newRefreshToken, userDTO);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    /**
     * Map User entity to UserDTO.
     */
    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .enabled(user.getEnabled())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .roles(user.getRoles().stream()
                .map(role -> role.getName().toString())
                .collect(java.util.stream.Collectors.toSet()))
            .build();
    }
}
