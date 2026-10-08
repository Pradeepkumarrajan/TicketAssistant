package com.example.tickets.llm;


public interface LlmClient {
    String complete(LlmRequest request);
}
