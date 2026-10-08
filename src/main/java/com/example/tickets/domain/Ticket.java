package com.example.tickets.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @Column(length = 36)
    private String id;

    /** Optimistic locking guards against concurrent status updates. */
    @Version
    private Long version;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    private String product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Embedded
    private TicketAnalysis analysis;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Ticket() { }

    public Ticket(String customerId, String subject, String description, Priority priority, String product) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.subject = subject;
        this.description = description;
        this.priority = priority;
        this.product = product;
        this.status = TicketStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void markProcessing() {
        this.status = TicketStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void complete(TicketAnalysis result) {
        this.analysis = result;
        this.status = TicketStatus.COMPLETED;
        this.failureReason = null;
        this.updatedAt = Instant.now();
    }

    public void fail(String reason) {
        this.analysis = null;
        this.status = TicketStatus.FAILED;
        this.failureReason = reason == null ? null : (reason.length() > 500 ? reason.substring(0, 500) : reason);
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public String getProduct() { return product; }
    public TicketStatus getStatus() { return status; }
    public TicketAnalysis getAnalysis() { return analysis; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
