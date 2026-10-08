package com.example.tickets.exception;

/** The LLM answered, but the answer is unusable. Never retried. */
public class InvalidLlmOutputException extends RuntimeException {
    public InvalidLlmOutputException(String message) {
        super(message);
    }
}
