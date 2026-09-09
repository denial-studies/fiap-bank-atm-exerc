package com.fiap.bank.atm.application.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record TransactionDTO(
        UUID id,
        LocalDateTime timestamp,
        String typeDescription,
        MoneyDTO amount,
        String description
) {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    public String formattedTimestamp() {
        return timestamp != null ? timestamp.format(TIMESTAMP_FORMATTER) : "";
    }
}
