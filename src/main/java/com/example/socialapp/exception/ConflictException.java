package com.example.socialapp.exception;

/**
 * Exception thrown when there is a conflict in the request (e.g., duplicate email).
 */
public class ConflictException extends RuntimeException {
    
    private final String conflictField;
    private final Object conflictValue;

    public ConflictException(String message, String conflictField, Object conflictValue) {
        super(message);
        this.conflictField = conflictField;
        this.conflictValue = conflictValue;
    }

    public ConflictException(String message) {
        super(message);
        this.conflictField = null;
        this.conflictValue = null;
    }

    public String getConflictField() {
        return conflictField;
    }

    public Object getConflictValue() {
        return conflictValue;
    }
}
