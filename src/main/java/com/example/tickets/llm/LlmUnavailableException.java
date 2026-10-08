package com.example.tickets.llm;

/** Temporary provider failure. Safe to retry. */
public class LlmUnavailableException extends LlmException {
    public LlmUnavailableException(String message) {
        super(message);
    }
}
