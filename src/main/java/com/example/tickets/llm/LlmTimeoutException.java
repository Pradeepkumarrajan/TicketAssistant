package com.example.tickets.llm;

public class LlmTimeoutException extends LlmUnavailableException {
    public LlmTimeoutException(String message) {
        super(message);
    }
}
