package com.fiap.bank.atm.application.dto;

public record AccountInfoDTO(
        String accountNumber,
        MoneyDTO balance,
        MoneyDTO dailyWithdrawalLimit,
        MoneyDTO totalWithdrawnToday,
        MoneyDTO remainingDailyLimit,
        boolean isBlocked
) {}
