package com.example.tickets.llm;

import com.example.tickets.domain.Ticket;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Builds the prompt from the templates in src/main/resources/prompts. */
@Component
public class PromptBuilder {

    private final String systemPrompt;
    private final String userTemplate;
    private final PiiRedactor redactor;

    public PromptBuilder(PiiRedactor redactor) throws IOException {
        this.redactor = redactor;
        this.systemPrompt = load("prompts/ticket-analysis-system.txt");
        this.userTemplate = load("prompts/ticket-analysis-user.txt");
    }

    public LlmRequest build(Ticket ticket) {
        String product = ticket.getProduct() == null ? "N/A" : ticket.getProduct();
        // Customer ID is intentionally never sent to the LLM.
        String user = userTemplate
                .replace("{{priority}}", ticket.getPriority().name())
                .replace("{{product}}", clean(product))
                .replace("{{subject}}", clean(ticket.getSubject()))
                .replace("{{description}}", clean(ticket.getDescription()));
        return new LlmRequest(systemPrompt, user);
    }

    /** Redacts PII and strips our delimiter tags so the ticket cannot "close" the data block early. */
    private String clean(String text) {
        return redactor.redact(text).replaceAll("(?i)</?ticket>", "");
    }

    private static String load(String path) throws IOException {
        return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
    }
}
