package com.example.tickets.exception;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String id) {
        super("Ticket not found: " + id);
    }
}
