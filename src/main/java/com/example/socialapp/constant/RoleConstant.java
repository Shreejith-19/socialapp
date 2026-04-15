package com.example.socialapp.constant;

/**
 * Role constants for the application.
 * Defines the three main user roles: USER, MODERATOR, and ADMIN.
 */
public class RoleConstant {
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_MODERATOR = "ROLE_MODERATOR";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    // Role names without ROLE_ prefix for easier comparison
    public static final String USER = "USER";
    public static final String MODERATOR = "MODERATOR";
    public static final String ADMIN = "ADMIN";

    private RoleConstant() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}
