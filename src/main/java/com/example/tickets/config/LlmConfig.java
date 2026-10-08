package com.example.tickets.config;

import com.example.tickets.llm.LlmClient;
import com.example.tickets.llm.MockLlmClient;
import com.example.tickets.llm.OpenAiCompatibleLlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
public class LlmConfig {

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "mock", matchIfMissing = true)
    public LlmClient mockLlmClient() {
        return new MockLlmClient();
    }

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
    public LlmClient openAiLlmClient(@Value("${llm.base-url}") String baseUrl,
                                     @Value("${llm.api-key:}") String apiKey,
                                     @Value("${llm.model}") String model,
                                     @Value("${llm.timeout-seconds:20}") long timeoutSeconds) {
        return new OpenAiCompatibleLlmClient(baseUrl, apiKey, model, Duration.ofSeconds(timeoutSeconds));
    }
}
