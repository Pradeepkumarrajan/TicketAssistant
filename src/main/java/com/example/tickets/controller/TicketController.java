package com.example.tickets.controller;

import com.example.tickets.dto.CreateTicketRequest;
import com.example.tickets.dto.TicketResponse;
import com.example.tickets.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse body = ticketService.create(request);
        return ResponseEntity.created(URI.create("/api/tickets/" + body.ticketId())).body(body);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable String id) {
        return ticketService.get(id);
    }
}
