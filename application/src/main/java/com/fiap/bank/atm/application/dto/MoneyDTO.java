package com.fiap.bank.atm.application.dto;

import com.fiap.bank.atm.domain.model.Money;
import java.math.BigDecimal;

public record MoneyDTO(BigDecimal amount, String formatted) {
    public static MoneyDTO of(Money money) {
        if (money == null) {
            return new MoneyDTO(BigDecimal.ZERO, "R$ 0,00");
        }
        return new MoneyDTO(money.getAmount(), money.format());
    }

    @Override
    public String toString() {
        return formatted;
    }
}
