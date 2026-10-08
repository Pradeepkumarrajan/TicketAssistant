package com.example.tickets.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Bridges "ticket committed" to the async analysis, keeping the request thread free. */
@Component
public class AnalysisTrigger {

    private final AnalysisService analysisService;

    public AnalysisTrigger(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @Async("analysisExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(TicketCreatedEvent event) {
        analysisService.analyze(event.ticketId());
    }
}
