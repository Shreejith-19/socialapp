package com.example.socialapp.frontend.util;

/**
 * Exception thrown when API returns error responses.
 */
public class ApiException extends Exception {
    private int statusCode;
    private String message;
    private String banType;
    private Long remainingDays;
    private Long remainingHours;
    private String banMessage;

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.message = message;
    }

    public ApiException(int statusCode, String message, String banType, 
                        Long remainingDays, Long remainingHours, String banMessage) {
        super(message);
        this.statusCode = statusCode;
        this.message = message;
        this.banType = banType;
        this.remainingDays = remainingDays;
        this.remainingHours = remainingHours;
        this.banMessage = banMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorMessage() {
        return message;
    }

    public boolean isBanned() {
        return statusCode == 403;
    }

    public boolean isUnauthorized() {
        return statusCode == 401;
    }

    public String getBanType() {
        return banType;
    }

    public Long getRemainingDays() {
        return remainingDays;
    }

    public Long getRemainingHours() {
        return remainingHours;
    }

    public String getBanMessage() {
        return banMessage;
    }
}
