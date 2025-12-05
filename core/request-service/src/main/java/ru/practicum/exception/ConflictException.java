package ru.practicum.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String entity, String reason) {
        super(String.format("%s conflict: %s", entity, reason));
    }
}




