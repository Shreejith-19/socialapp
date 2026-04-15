package com.example.socialapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Social Media Content Moderation System Application.
 * 
 * Main entry point for the Spring Boot application.
 * Features:
 * - JWT-based authentication and authorization
 * - Role-based access control (USER, MODERATOR, ADMIN)
 * - Content moderation system
 * - RESTful API endpoints
 * - H2 in-memory database for development
 * 
 * @author Development Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.socialapp.repository")
@ComponentScan(basePackages = "com.example.socialapp")
public class SocialappApplication {

	public static void main(String[] args) {
		SpringApplication.run(SocialappApplication.class, args);
	}

}
