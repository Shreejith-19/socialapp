package com.example.socialapp.constant;

/**
 * API constants for the application.
 * Contains API endpoint paths and messaging constants.
 */
public class ApiConstant {
    // API Endpoints
    public static final String API_V1 = "/v1";
    public static final String AUTH_ENDPOINT = API_V1 + "/auth";
    public static final String CONTENT_ENDPOINT = API_V1 + "/content";
    public static final String USER_ENDPOINT = API_V1 + "/users";
    public static final String MODERATION_ENDPOINT = API_V1 + "/moderation";

    // HTTP Headers
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String X_REQUEST_ID = "X-Request-ID";

    // Response Messages
    public static final String SUCCESS_MESSAGE = "Request processed successfully";
    public static final String ERROR_MESSAGE = "An error occurred while processing your request";
    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized access";
    public static final String FORBIDDEN_MESSAGE = "Forbidden access";
    public static final String NOT_FOUND_MESSAGE = "Resource not found";

    private ApiConstant() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}
