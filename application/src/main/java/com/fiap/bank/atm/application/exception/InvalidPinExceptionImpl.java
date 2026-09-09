package com.fiap.bank.atm.application.exception;

import com.fiap.bank.atm.domain.exception.InvalidPinException;

public class InvalidPinExceptionImpl extends InvalidPinException {
    public InvalidPinExceptionImpl(String message) {
        super(message);
    }
}
