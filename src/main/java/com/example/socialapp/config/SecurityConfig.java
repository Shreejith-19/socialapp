package com.example.socialapp.config;

import com.example.socialapp.security.CustomUserDetailsService;
import com.example.socialapp.security.JwtAuthenticationFilter;
import com.example.socialapp.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * Security Configuration.
 * Configures Spring Security with JWT authentication, CORS, and comprehensive method-level security.
 * 
 * Role-Based Access Control (RBAC):
 * - USER: Can create posts, comments, and likes/dislikes. Can submit appeals.
 * - MODERATOR: Can review flagged content, make moderation decisions, and review appeals.
 * - ADMIN: Has full access to user management and can finalize appeals.
 * 
 * All endpoints are secured via @PreAuthorize annotations on controller methods
 * and HttpSecurity authorization rules for additional layer of security.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Password encoder bean.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication provider bean.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication manager bean.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfiguration) throws Exception {
        return authConfiguration.getAuthenticationManager();
    }

    /**
     * JWT Authentication filter bean.
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService);
    }

    /**
     * CORS configuration.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:4200",
            "http://localhost:8080"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Security filter chain configuration.
     * Implements comprehensive role-based access control:
     * - USER: Create posts, comments, likes/dislikes, and submit appeals
     * - MODERATOR: Review flagged content and make moderation decisions
     * - ADMIN: User management and appeal finalization
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(authz -> authz
            // Public endpoints - no authentication required
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/v1/auth/**").permitAll()
            .requestMatchers("/h2-console/**").permitAll()
            .requestMatchers("/v1/health").permitAll()
            
            // USER role: Post creation and interaction (like, comment)
            .requestMatchers(HttpMethod.POST, "/v1/posts").hasAnyRole("USER", "MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/v1/posts/*/like").hasAnyRole("USER", "MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/v1/posts/*/dislike").hasAnyRole("USER", "MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/v1/posts/*/like").hasAnyRole("USER", "MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/v1/comments").hasAnyRole("USER", "MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.PUT, "/v1/comments/**").hasAnyRole("USER", "MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/v1/comments/**").hasAnyRole("USER", "MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/v1/appeals").hasAnyRole("USER", "MODERATOR", "ADMIN")
            
            // Read endpoints - authenticated users
            .requestMatchers(HttpMethod.GET, "/v1/posts/**").hasAnyRole("USER", "MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.GET, "/v1/comments/**").hasAnyRole("USER", "MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.GET, "/v1/users/me").hasAnyRole("USER", "MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.PUT, "/v1/users/me").hasAnyRole("USER", "MODERATOR", "ADMIN")
            
            // MODERATOR role: Content moderation
            .requestMatchers(HttpMethod.GET, "/v1/moderation/**").hasAnyRole("MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/v1/moderation/**").hasAnyRole("MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.PUT, "/v1/moderation/**").hasAnyRole("MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.GET, "/v1/reports/**").hasAnyRole("MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.PUT, "/v1/reports/**").hasAnyRole("MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.GET, "/v1/appeals/pending").hasAnyRole("MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.GET, "/v1/appeals/status/**").hasAnyRole("MODERATOR", "ADMIN")
            .requestMatchers(HttpMethod.PUT, "/v1/appeals/*/review").hasAnyRole("MODERATOR", "ADMIN")
            
            // ADMIN role: User management and appeal finalization
            .requestMatchers(HttpMethod.GET, "/v1/users/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/v1/users/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/v1/users/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/v1/posts/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/v1/appeals/*/decision").hasRole("ADMIN")
            .requestMatchers("/v1/admin/**").hasRole("ADMIN")
            
            // All other requests require authentication
            .anyRequest().authenticated()
        );

        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        return http.build();
    }
}
