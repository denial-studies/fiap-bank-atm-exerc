package com.fiap.bank.atm.application.exception;

import com.fiap.bank.atm.domain.exception.DailyLimitExceededException;

public class DailyLimitExceededExceptionImpl extends DailyLimitExceededException {
    public DailyLimitExceededExceptionImpl(String message) {
        super(message);
    }
}
