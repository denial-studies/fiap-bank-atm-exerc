package com.fiap.bank.atm.infrastructure.persistence;

import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.infrastructure.factory.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Responsável por fornecer a estrutura das tabelas (scripts DDL) e os objetos
 * Java
 * das contas iniciais do sistema com seus históricos.
 * Não implementa mais AccountRepository.
 */
public class InMemoryAccountRepository {

    public static final String CREATE_TABLE_ACCOUNT_SQL = """
                CREATE TABLE IF NOT EXISTS tb_account (
                    id VARCHAR(36) PRIMARY KEY,
                    agency VARCHAR(10) NOT NULL DEFAULT '0001',
                    number VARCHAR(20) NOT NULL UNIQUE,
                    balance DECIMAL(15, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    pin VARCHAR(10) NOT NULL DEFAULT '1234',
                    daily_limit DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
                    failed_attempts INTEGER NOT NULL DEFAULT 0
                );
            """;

    public static final String CREATE_TABLE_TRANSACTION_SQL = """
                CREATE TABLE IF NOT EXISTS tb_transaction (
                    id VARCHAR(36) PRIMARY KEY,
                    account_id VARCHAR(36) NOT NULL,
                    type VARCHAR(20) NOT NULL,
                    amount DECIMAL(15, 2) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    description TEXT,
                    FOREIGN KEY (account_id) REFERENCES tb_account(id) ON DELETE CASCADE
                );
            """;

    /**
     * Executa a criação da estrutura de tabelas no banco de dados SQLite.
     */
    public void createTables() {
        try (Connection conn = ConnectionFactory.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_ACCOUNT_SQL);
            stmt.execute(CREATE_TABLE_TRANSACTION_SQL);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar estrutura de tabelas no banco de dados SQLite.", e);
        }
    }

    public String getCreateTableAccountSql() {
        return CREATE_TABLE_ACCOUNT_SQL;
    }

    public String getCreateTableTransactionSql() {
        return CREATE_TABLE_TRANSACTION_SQL;
    }

    /**
     * Cria e retorna os objetos Java das contas iniciais com seus respectivos
     * históricos de transações.
     */
    public List<Account> createInitialAccounts() {
        List<Account> accounts = new ArrayList<>();

        // Conta 1
        Account acc1 = new Account(
                UUID.randomUUID(),
                "12345",
                "1234",
                Money.of(5000.00),
                Money.of(1500.00));
        acc1.seedTransaction(new Transaction(
                UUID.randomUUID(),
                LocalDateTime.now().minusDays(3),
                TransactionType.DEPOSIT,
                Money.of(2000.00),
                "Depósito em dinheiro"));
        acc1.seedTransaction(new Transaction(
                UUID.randomUUID(),
                LocalDateTime.now().minusDays(2),
                TransactionType.TRANSFER_IN,
                Money.of(500.00),
                "Transf. de Conta 67890"));
        acc1.seedTransaction(new Transaction(
                UUID.randomUUID(),
                LocalDateTime.now().minusDays(1),
                TransactionType.WITHDRAWAL,
                Money.of(100.00),
                "Saque eletrônico"));
        accounts.add(acc1);

        // Conta 2
        Account acc2 = new Account(
                UUID.randomUUID(),
                "67890",
                "5678",
                Money.of(1200.00),
                Money.of(1000.00));
        acc2.seedTransaction(new Transaction(
                UUID.randomUUID(),
                LocalDateTime.now().minusDays(5),
                TransactionType.DEPOSIT,
                Money.of(1500.00),
                "Depósito inicial"));
        acc2.seedTransaction(new Transaction(
                UUID.randomUUID(),
                LocalDateTime.now().minusDays(2),
                TransactionType.TRANSFER_OUT,
                Money.of(500.00),
                "Transf. para Conta 12345"));
        accounts.add(acc2);

        // Conta 3
        Account acc3 = new Account(
                UUID.randomUUID(),
                "99999",
                "9999",
                Money.of(50.00),
                Money.of(500.00));
        acc3.seedTransaction(new Transaction(
                UUID.randomUUID(),
                LocalDateTime.now().minusDays(10),
                TransactionType.DEPOSIT,
                Money.of(50.00),
                "Abertura de conta"));
        accounts.add(acc3);

        return accounts;
    }

    public List<Account> getInitialAccounts() {
        return createInitialAccounts();
    }
}
