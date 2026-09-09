package com.fiap.bank.atm.application.exception;

import com.fiap.bank.atm.domain.exception.AccountBlockedException;

public class AccountBlockedExceptionImpl extends AccountBlockedException {
    public AccountBlockedExceptionImpl(String message) {
        super(message);
    }
}
