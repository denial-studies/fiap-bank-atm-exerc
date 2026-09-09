package com.fiap.bank.atm.application.service;

import com.fiap.bank.atm.application.dto.AccountInfoDTO;
import com.fiap.bank.atm.application.dto.MoneyDTO;
import com.fiap.bank.atm.application.dto.TransactionDTO;
import com.fiap.bank.atm.application.exception.AccountBlockedExceptionImpl;
import com.fiap.bank.atm.application.exception.DailyLimitExceededExceptionImpl;
import com.fiap.bank.atm.application.exception.InsufficientFundsExceptionImpl;
import com.fiap.bank.atm.application.exception.InvalidPinExceptionImpl;
import com.fiap.bank.atm.domain.exception.AccountBlockedException;
import com.fiap.bank.atm.domain.exception.DailyLimitExceededException;
import com.fiap.bank.atm.domain.exception.InsufficientFundsException;
import com.fiap.bank.atm.domain.exception.InvalidPinException;
import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import com.fiap.bank.atm.infrastructure.persistence.InMemoryAccountRepository;

import java.util.List;

public class AtmService {
    private final AccountRepository accountRepository;
    private Account currentAccount;

    public AtmService() {
        this(new InMemoryAccountRepository());
    }

    public AtmService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountInfoDTO authenticate(String accountNumber, String pin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new InvalidPinExceptionImpl("Conta não encontrada."));

        try {
            account.authenticate(pin);
            currentAccount = account;
            return toDTO(account);
        } catch (AccountBlockedException e) {
            accountRepository.save(account);
            throw new AccountBlockedExceptionImpl(e.getMessage());
        } catch (InvalidPinException e) {
            accountRepository.save(account);
            throw new InvalidPinExceptionImpl(e.getMessage());
        }
    }

    public void withdraw(double amount) {
        ensureAuthenticated();
        try {
            currentAccount.withdraw(Money.of(amount));
            accountRepository.save(currentAccount);
        } catch (AccountBlockedException e) {
            throw new AccountBlockedExceptionImpl(e.getMessage());
        } catch (InsufficientFundsException e) {
            throw new InsufficientFundsExceptionImpl(e.getMessage());
        } catch (DailyLimitExceededException e) {
            throw new DailyLimitExceededExceptionImpl(e.getMessage());
        }
    }

    public void deposit(double amount) {
        ensureAuthenticated();
        try {
            currentAccount.deposit(Money.of(amount));
            accountRepository.save(currentAccount);
        } catch (AccountBlockedException e) {
            throw new AccountBlockedExceptionImpl(e.getMessage());
        }
    }

    public void transfer(String targetAccountNumber, double amount) {
        ensureAuthenticated();

        Account targetAccount = accountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada."));

        try {
            currentAccount.transfer(targetAccount, Money.of(amount));
            accountRepository.save(currentAccount);
            accountRepository.save(targetAccount);
        } catch (AccountBlockedException e) {
            throw new AccountBlockedExceptionImpl(e.getMessage());
        } catch (InsufficientFundsException e) {
            throw new InsufficientFundsExceptionImpl(e.getMessage());
        }
    }

    public MoneyDTO getBalance() {
        ensureAuthenticated();
        return MoneyDTO.of(currentAccount.getBalance());
    }

    public List<TransactionDTO> getStatement() {
        ensureAuthenticated();
        return currentAccount.getTransactions().stream()
                .map(tx -> new TransactionDTO(
                        tx.getId(),
                        tx.getTimestamp(),
                        tx.getType().getDescription(),
                        MoneyDTO.of(tx.getAmount()),
                        tx.getDescription()
                ))
                .toList();
    }

    public void logout() {
        currentAccount = null;
    }

    public AccountInfoDTO getCurrentAccount() {
        return currentAccount != null ? toDTO(currentAccount) : null;
    }

    public boolean isAuthenticated() {
        return currentAccount != null;
    }

    private void ensureAuthenticated() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário está autenticado no momento.");
        }
    }

    private AccountInfoDTO toDTO(Account acc) {
        return new AccountInfoDTO(
                acc.getAccountNumber(),
                MoneyDTO.of(acc.getBalance()),
                MoneyDTO.of(acc.getDailyWithdrawalLimit()),
                MoneyDTO.of(acc.getTotalWithdrawnToday()),
                MoneyDTO.of(acc.getDailyWithdrawalLimit().minus(acc.getTotalWithdrawnToday())),
                acc.isBlocked()
        );
    }
}
