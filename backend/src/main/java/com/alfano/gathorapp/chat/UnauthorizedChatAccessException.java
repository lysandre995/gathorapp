package com.alfano.gathorapp.chat;

/**
 * Exception thrown when a user tries to access or modify a chat without proper
 * permissions.
 */
public class UnauthorizedChatAccessException extends RuntimeException {

    public UnauthorizedChatAccessException(String message) {
        super(message);
    }

    public UnauthorizedChatAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
