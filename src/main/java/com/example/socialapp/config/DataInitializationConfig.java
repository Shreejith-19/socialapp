package com.example.socialapp.config;

import com.example.socialapp.entity.Role;
import com.example.socialapp.entity.User;
import com.example.socialapp.repository.RoleRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.enums.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;

/**
 * Database Initialization Configuration.
 * Seeds default roles and test users into the database on application startup.
 */
@Slf4j
@Configuration
public class DataInitializationConfig {

    /**
     * Initialize default roles and test users in a single transaction.
     * This ensures roles are created before test users try to reference them.
     */
    @Bean
    public CommandLineRunner initializeDatabase(UserRepository userRepository, RoleRepository roleRepository, 
                                                 PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("Initializing database...");

            // Initialize default roles first
            log.info("Initializing default roles...");

            // Check and create USER role
            Role userRole;
            if (roleRepository.findByName(Role.RoleType.USER).isEmpty()) {
                userRole = new Role();
                userRole.setName(Role.RoleType.USER);
                userRole.setDescription("Regular user role");
                userRole = roleRepository.save(userRole);
                log.info("USER role created");
            } else {
                userRole = roleRepository.findByName(Role.RoleType.USER).get();
            }

            // Check and create MODERATOR role
            Role moderatorRole;
            if (roleRepository.findByName(Role.RoleType.MODERATOR).isEmpty()) {
                moderatorRole = new Role();
                moderatorRole.setName(Role.RoleType.MODERATOR);
                moderatorRole.setDescription("Moderator role with content review permissions");
                moderatorRole = roleRepository.save(moderatorRole);
                log.info("MODERATOR role created");
            } else {
                moderatorRole = roleRepository.findByName(Role.RoleType.MODERATOR).get();
            }

            // Check and create ADMIN role
            Role adminRole;
            if (roleRepository.findByName(Role.RoleType.ADMIN).isEmpty()) {
                adminRole = new Role();
                adminRole.setName(Role.RoleType.ADMIN);
                adminRole.setDescription("Administrator role with full system access");
                adminRole = roleRepository.save(adminRole);
                log.info("ADMIN role created");
            } else {
                adminRole = roleRepository.findByName(Role.RoleType.ADMIN).get();
            }

            log.info("Default roles initialization completed");

            // Initialize test users after roles are committed
            log.info("Initializing test users...");

            // Create test MODERATOR user
            if (userRepository.findByEmail("moderator@test.com").isEmpty()) {
                User moderatorUser = new User();
                moderatorUser.setEmail("moderator@test.com");
                moderatorUser.setPassword(passwordEncoder.encode("moderator123"));
                moderatorUser.setFirstName("Mod");
                moderatorUser.setLastName("User");
                moderatorUser.setUsername("moderator");
                moderatorUser.setAge(30);
                moderatorUser.setStatus(UserStatus.ACTIVE);
                moderatorUser.setCreatedAt(LocalDateTime.now());
                moderatorUser.getRoles().add(moderatorRole);
                
                userRepository.save(moderatorUser);
                log.info("✅ Test MODERATOR user created - Email: moderator@test.com, Password: moderator123");
            }

            // Create test ADMIN user
            if (userRepository.findByEmail("admin@test.com").isEmpty()) {
                User adminUser = new User();
                adminUser.setEmail("admin@test.com");
                adminUser.setPassword(passwordEncoder.encode("admin123"));
                adminUser.setFirstName("Admin");
                adminUser.setLastName("User");
                adminUser.setUsername("admin");
                adminUser.setAge(35);
                adminUser.setStatus(UserStatus.ACTIVE);
                adminUser.setCreatedAt(LocalDateTime.now());
                adminUser.getRoles().add(adminRole);
                
                userRepository.save(adminUser);
                log.info("✅ Test ADMIN user created - Email: admin@test.com, Password: admin123");
            }

            log.info("Test users initialization completed");
        };
    }
}
