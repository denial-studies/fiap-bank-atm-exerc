package com.fiap.bank.atm.application.exception;

import com.fiap.bank.atm.domain.exception.InsufficientFundsException;

public class InsufficientFundsExceptionImpl extends InsufficientFundsException {
    public InsufficientFundsExceptionImpl(String message) {
        super(message);
    }
}
