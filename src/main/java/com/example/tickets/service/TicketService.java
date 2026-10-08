package com.example.tickets.service;

import com.example.tickets.domain.Ticket;
import com.example.tickets.dto.CreateTicketRequest;
import com.example.tickets.dto.TicketResponse;
import com.example.tickets.exception.TicketNotFoundException;
import com.example.tickets.repository.TicketRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository repository;
    private final ApplicationEventPublisher publisher;

    public TicketService(TicketRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * Persists the ticket and publishes an event. The async analysis only starts AFTER the
     * transaction commits (see AnalysisTrigger), so it can never see a missing ticket.
     */
    @Transactional
    public TicketResponse create(CreateTicketRequest request) {
        Ticket ticket = repository.save(new Ticket(
                request.customerId().trim(),
                request.subject().trim(),
                request.description().trim(),
                request.priority(),
                request.product() == null || request.product().isBlank() ? null : request.product().trim()));
        publisher.publishEvent(new TicketCreatedEvent(ticket.getId()));
        return TicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public TicketResponse get(String id) {
        return repository.findById(id).map(TicketResponse::from)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }
}
