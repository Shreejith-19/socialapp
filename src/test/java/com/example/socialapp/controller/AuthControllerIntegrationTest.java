package com.example.socialapp.controller;

import com.example.socialapp.BaseIntegrationTest;
import com.example.socialapp.dto.AuthRequest;
import com.example.socialapp.dto.UserDTO;
import com.example.socialapp.entity.Role;
import com.example.socialapp.entity.User;
import com.example.socialapp.repository.RoleRepository;
import com.example.socialapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for AuthController.
 */
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        // Role might already exist from initialization, so just get it
        userRole = roleRepository.findByName(Role.RoleType.USER)
            .orElseGet(() -> {
                Role role = new Role();
                role.setName(Role.RoleType.USER);
                role.setDescription("User role");
                return roleRepository.save(role);
            });
    }

    @Test
    void testRegisterUser() throws Exception {
        UserDTO userDTO = UserDTO.builder()
            .email("newuser@example.com")
            .firstName("John")
            .lastName("Doe")
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(userDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.roles", hasItem("USER")))
            .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void testRegisterUserWithDuplicateEmail() throws Exception {
        // Create first user
        User existingUser = new User();
        existingUser.setEmail("duplicate@example.com");
        existingUser.setPassword(passwordEncoder.encode("password"));
        existingUser.setFirstName("Existing");
        existingUser.setLastName("User");
        existingUser.setRoles(Collections.singleton(userRole));
        userRepository.save(existingUser);

        // Try to register with same email
        UserDTO userDTO = UserDTO.builder()
            .email("duplicate@example.com")
            .firstName("New")
            .lastName("User")
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(userDTO)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void testLoginSuccess() throws Exception {
        // Create a test user
        User testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRoles(Collections.singleton(userRole));
        userRepository.save(testUser);

        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(authRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("nonexistent@example.com");
        authRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(authRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testValidateToken() throws Exception {
        // Create and login a user
        User testUser = new User();
        testUser.setEmail("validate@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("Validate");
        testUser.setLastName("User");
        testUser.setRoles(Collections.singleton(userRole));
        userRepository.save(testUser);

        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("validate@example.com");
        authRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(authRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String accessToken = jsonNode.get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/validate-token")
            .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/auth/health"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("running")));
    }
}
