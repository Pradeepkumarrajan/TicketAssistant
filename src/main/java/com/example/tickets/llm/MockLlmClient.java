package com.example.tickets.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic keyword-based stand-in for a real model, so the project runs with no API key.
 * Demo triggers: put [mock-timeout] or [mock-invalid] in a ticket to simulate those failures.
 */
public class MockLlmClient implements LlmClient {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String complete(LlmRequest request) {
        String text = request.userPrompt().toLowerCase(Locale.ROOT);

        if (text.contains("[mock-timeout]")) {
            throw new LlmTimeoutException("Mock provider simulated a timeout");
        }
        if (text.contains("[mock-invalid]")) {
            return "Sorry, I cannot help with that.";
        }
        if (text.contains("import")) {
            return reply("IMPORT_FAILURE", "IMPORT_ENGINEERING",
                    "The customer is experiencing a document import failure.",
                    "Thank you for reporting this. We are investigating the import issue and will update you shortly.",
                    0.87);
        }
        if (containsAny(text, "invoice", "billing", "refund", "charge", "payment")) {
            return reply("BILLING_ISSUE", "BILLING_SUPPORT",
                    "The customer has a question or problem related to billing.",
                    "Thank you for contacting us. Our billing team is reviewing your account and will follow up.",
                    0.82);
        }
        if (containsAny(text, "login", "log in", "password", "sign in", "account")) {
            return reply("ACCOUNT_ACCESS", "ACCOUNT_SUPPORT",
                    "The customer has trouble accessing or managing their account.",
                    "Thanks for reaching out. Our account team will help you regain access.",
                    0.8);
        }
        if (containsAny(text, "crash", "slow", "error", "outage", "down")) {
            return reply("PLATFORM_ISSUE", "PLATFORM_ENGINEERING",
                    "The customer reports a performance or stability problem.",
                    "We are sorry for the trouble. Our engineers are looking into the issue now.",
                    0.75);
        }
        return reply("GENERAL_INQUIRY", "GENERAL_SUPPORT",
                "The customer submitted a general support request.",
                "Thank you for contacting support. A team member will review your request and respond soon.",
                0.55);
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private String reply(String category, String team, String summary, String response, double confidence) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("category", category);
        body.put("summary", summary);
        body.put("suggestedResponse", response);
        body.put("recommendedTeam", team);
        body.put("confidence", confidence);
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
