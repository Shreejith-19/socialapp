package com.example.socialapp.exception;

import lombok.Getter;

/**
 * Exception thrown when a banned user attempts to perform a restricted action.
 * Includes ban information such as remaining time for temporary bans.
 */
@Getter
public class BannedUserException extends RuntimeException {
    private final long remainingDays;
    private final long remainingHours;
    private final String banMessage;
    private final boolean isTemporary;

    public BannedUserException(String message, long remainingDays, long remainingHours, String banMessage, boolean isTemporary) {
        super(message);
        this.remainingDays = remainingDays;
        this.remainingHours = remainingHours;
        this.banMessage = banMessage;
        this.isTemporary = isTemporary;
    }
}
