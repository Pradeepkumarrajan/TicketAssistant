package com.example.tickets.dto;

import com.example.tickets.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 100) String customerId,
        @NotBlank @Size(max = 255) String subject,
        @NotBlank @Size(max = 5000) String description,
        @NotNull Priority priority,
        @Size(max = 100) String product) { }
