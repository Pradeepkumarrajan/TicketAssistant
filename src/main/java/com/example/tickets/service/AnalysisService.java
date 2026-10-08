package com.example.tickets.service;

import com.example.tickets.domain.Ticket;
import com.example.tickets.domain.TicketAnalysis;
import com.example.tickets.domain.TicketStatus;
import com.example.tickets.exception.InvalidLlmOutputException;
import com.example.tickets.llm.AnalysisParser;
import com.example.tickets.llm.LlmClient;
import com.example.tickets.llm.LlmException;
import com.example.tickets.llm.LlmRequest;
import com.example.tickets.llm.LlmUnavailableException;
import com.example.tickets.llm.PromptBuilder;
import com.example.tickets.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Runs one analysis. Deliberately NOT @Transactional: no DB transaction is held open while
 * waiting on the LLM. Never throws; every outcome ends as COMPLETED or FAILED.
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final TicketRepository repository;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final AnalysisParser parser;
    private final int maxAttempts;
    private final long backoffMs;

    public AnalysisService(TicketRepository repository, LlmClient llmClient, PromptBuilder promptBuilder,
                           AnalysisParser parser,
                           @Value("${llm.retry.max-attempts:3}") int maxAttempts,
                           @Value("${llm.retry.backoff-ms:500}") long backoffMs) {
        this.repository = repository;
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.backoffMs = Math.max(0, backoffMs);
    }

    public void analyze(String ticketId) {
        Ticket ticket = repository.findById(ticketId).orElse(null);
        if (ticket == null) {
            log.warn("Analysis skipped, ticket not found ticketId={}", ticketId);
            return;
        }
        if (ticket.getStatus() != TicketStatus.PENDING) {
            log.info("Analysis skipped, ticket not pending ticketId={} status={}", ticketId, ticket.getStatus());
            return;
        }

        ticket.markProcessing();
        ticket = repository.save(ticket);

        long start = System.nanoTime();
        try {
            LlmRequest request = promptBuilder.build(ticket);
            String raw = callWithRetry(request);
            TicketAnalysis analysis = parser.parse(raw);
            ticket.complete(analysis);
            log.info("Analysis completed ticketId={} latencyMs={}", ticketId, elapsedMs(start));
        } catch (InvalidLlmOutputException e) {
            ticket.fail("LLM returned invalid output: " + e.getMessage());
            log.warn("Analysis failed, invalid LLM output ticketId={} reason={}", ticketId, e.getMessage());
        } catch (LlmException e) {
            ticket.fail("LLM provider failure: " + e.getMessage());
            log.warn("Analysis failed, provider error ticketId={} reason={} latencyMs={}",
                    ticketId, e.getMessage(), elapsedMs(start));
        } catch (RuntimeException e) {
            ticket.fail("Unexpected error during analysis");
            log.error("Analysis failed unexpectedly ticketId={}", ticketId, e);
        }
        repository.save(ticket);
    }

    /** Retries only temporary failures (timeouts, 429, 5xx) with exponential backoff. */
    private String callWithRetry(LlmRequest request) {
        LlmUnavailableException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return llmClient.complete(request);
            } catch (LlmUnavailableException e) {
                last = e;
                log.warn("LLM temporary failure attempt={}/{} reason={}", attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    sleep(backoffMs * (1L << (attempt - 1)));
                }
            }
        }
        throw last;
    }

    private void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted while waiting to retry");
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
