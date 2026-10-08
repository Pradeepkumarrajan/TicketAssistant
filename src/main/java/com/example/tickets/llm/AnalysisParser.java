package com.example.tickets.llm;

import com.example.tickets.domain.RecommendedTeam;
import com.example.tickets.domain.TicketAnalysis;
import com.example.tickets.exception.InvalidLlmOutputException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Parses and strictly validates raw LLM text. Anything unexpected becomes InvalidLlmOutputException. */
@Component
public class AnalysisParser {

    private static final Pattern CATEGORY = Pattern.compile("^[A-Z][A-Z0-9_]{2,49}$");

    record Raw(String category, String summary, String suggestedResponse,
               String recommendedTeam, Double confidence) { }

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    public TicketAnalysis parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new InvalidLlmOutputException("empty response");
        }
        // Tolerate code fences or a sentence around the JSON by taking the outermost braces.
        int start = rawText.indexOf('{');
        int end = rawText.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new InvalidLlmOutputException("response is not valid JSON");
        }

        Raw raw;
        try {
            raw = mapper.readValue(rawText.substring(start, end + 1), Raw.class);
        } catch (JsonProcessingException e) {
            throw new InvalidLlmOutputException("response is not valid JSON or contains unexpected fields");
        }

        String category = text(raw.category(), "category", 1, 50);
        if (!CATEGORY.matcher(category).matches()) {
            throw new InvalidLlmOutputException("category must be UPPER_SNAKE_CASE (3-50 chars)");
        }
        String summary = text(raw.summary(), "summary", 1, 1000);
        String suggestedResponse = text(raw.suggestedResponse(), "suggestedResponse", 1, 4000);

        RecommendedTeam team;
        try {
            team = RecommendedTeam.valueOf(text(raw.recommendedTeam(), "recommendedTeam", 1, 50));
        } catch (IllegalArgumentException e) {
            throw new InvalidLlmOutputException("recommendedTeam is not an allowed value");
        }

        Double confidence = raw.confidence();
        if (confidence == null || confidence.isNaN() || confidence < 0 || confidence > 1) {
            throw new InvalidLlmOutputException("confidence must be a number between 0 and 1");
        }

        return new TicketAnalysis(category, summary, suggestedResponse, team.name(), confidence);
    }

    private static String text(String value, String field, int min, int max) {
        if (value == null || value.isBlank()) {
            throw new InvalidLlmOutputException(field + " is missing or blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() < min || trimmed.length() > max) {
            throw new InvalidLlmOutputException(field + " has an invalid length");
        }
        return trimmed;
    }
}
