package com.example.tickets.dto;

import com.example.tickets.domain.Priority;
import com.example.tickets.domain.Ticket;
import com.example.tickets.domain.TicketAnalysis;
import com.example.tickets.domain.TicketStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketResponse(
        String ticketId,
        String customerId,
        String subject,
        String description,
        Priority priority,
        String product,
        TicketStatus status,
        Instant createdAt,
        Instant updatedAt,
        String failureReason,
        Analysis analysis) {

    public record Analysis(String category, String summary, String suggestedResponse,
                           String recommendedTeam, Double confidence) { }

    public static TicketResponse from(Ticket t) {
        TicketAnalysis a = t.getAnalysis();
        Analysis analysis = (a == null || a.getCategory() == null) ? null
                : new Analysis(a.getCategory(), a.getSummary(), a.getSuggestedResponse(),
                a.getRecommendedTeam(), a.getConfidence());
        return new TicketResponse(t.getId(), t.getCustomerId(), t.getSubject(), t.getDescription(),
                t.getPriority(), t.getProduct(), t.getStatus(), t.getCreatedAt(), t.getUpdatedAt(),
                t.getFailureReason(), analysis);
    }
}
