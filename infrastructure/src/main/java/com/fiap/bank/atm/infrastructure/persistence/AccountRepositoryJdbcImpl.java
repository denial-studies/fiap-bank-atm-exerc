package com.fiap.bank.atm.infrastructure.persistence;

import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import com.fiap.bank.atm.infrastructure.factory.ConnectionFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AccountRepositoryJdbcImpl implements AccountRepository {

    public AccountRepositoryJdbcImpl() {
    }

    private String normalizeAccountNumber(String accountNumber) {
        if (accountNumber == null) {
            return "";
        }
        String cleaned = accountNumber.replaceAll("[^0-9]", "");
        return cleaned.isEmpty() ? accountNumber.trim() : cleaned;
    }

    @Override
    public void save(Account account) {
        if (account == null) {
            return;
        }

        String updateAccountSql = """
            UPDATE tb_account
            SET balance = ?, status = ?
            WHERE id = ?;
        """;

        String insertAccountSql = """
            INSERT INTO tb_account (id, agency, number, balance, status, pin, daily_limit, failed_attempts)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """;

        String insertTransactionSql = """
            INSERT OR IGNORE INTO tb_transaction (id, account_id, type, amount, created_at, description)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        String normalizedNumber = normalizeAccountNumber(account.getAccountNumber());
        String status = account.isBlocked() ? "BLOCKED" : "ACTIVE";

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Tenta atualizar a conta (ex: atualização de saldo após transação)
                int rowsUpdated = 0;
                try (PreparedStatement stmtUpdate = conn.prepareStatement(updateAccountSql)) {
                    stmtUpdate.setBigDecimal(1, account.getBalance().getAmount());
                    stmtUpdate.setString(2, status);
                    stmtUpdate.setString(3, account.getId().toString());
                    rowsUpdated = stmtUpdate.executeUpdate();
                }

                // Se a conta ainda não existe (ex: carga inicial), insere
                if (rowsUpdated == 0) {
                    try (PreparedStatement stmtInsert = conn.prepareStatement(insertAccountSql)) {
                        stmtInsert.setString(1, account.getId().toString());
                        stmtInsert.setString(2, "0001");
                        stmtInsert.setString(3, normalizedNumber);
                        stmtInsert.setBigDecimal(4, account.getBalance().getAmount());
                        stmtInsert.setString(5, status);
                        stmtInsert.setString(6, account.getPin() != null ? account.getPin() : "1234");
                        stmtInsert.setBigDecimal(7, account.getDailyWithdrawalLimit().getAmount());
                        stmtInsert.setInt(8, account.getFailedAttempts());
                        stmtInsert.executeUpdate();
                    }
                }

                // Insere as transações da conta
                try (PreparedStatement stmtTx = conn.prepareStatement(insertTransactionSql)) {
                    for (Transaction tx : account.getTransactions()) {
                        stmtTx.setString(1, tx.getId().toString());
                        stmtTx.setString(2, account.getId().toString());
                        stmtTx.setString(3, tx.getType().name());
                        stmtTx.setBigDecimal(4, tx.getAmount().getAmount());
                        stmtTx.setString(5, tx.getTimestamp().toString());
                        stmtTx.setString(6, tx.getDescription() != null ? tx.getDescription() : "");
                        stmtTx.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Erro ao salvar conta e transações na base de dados.", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha de conexão ao persistir conta: " + account.getAccountNumber(), e);
        }
    }

    @Override
    public Optional<Account> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }

        String sql = """
            SELECT id, agency, number, balance, status, pin, daily_limit, failed_attempts
            FROM tb_account
            WHERE id = ?;
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Account account = new Account(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("number"),
                        rs.getString("pin"),
                        Money.of(rs.getBigDecimal("balance")),
                        Money.of(rs.getBigDecimal("daily_limit"))
                    );
                    loadTransactions(account);
                    return Optional.of(account);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar conta por ID: " + id, e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        if (accountNumber == null) {
            return Optional.empty();
        }

        String rawNumber = accountNumber.trim();
        String normalizedNumber = normalizeAccountNumber(accountNumber);

        String sql = """
            SELECT id, agency, number, balance, status, pin, daily_limit, failed_attempts
            FROM tb_account
            WHERE number = ? OR number = ?;
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, rawNumber);
            stmt.setString(2, normalizedNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Account account = new Account(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("number"),
                        rs.getString("pin"),
                        Money.of(rs.getBigDecimal("balance")),
                        Money.of(rs.getBigDecimal("daily_limit"))
                    );
                    loadTransactions(account);
                    return Optional.of(account);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar conta por número: " + accountNumber, e);
        }

        return Optional.empty();
    }

    @Override
    public List<Account> findAll() {
        List<Account> accounts = new ArrayList<>();
        String sql = """
            SELECT id, agency, number, balance, status, pin, daily_limit, failed_attempts
            FROM tb_account
            ORDER BY number ASC;
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Account account = new Account(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("number"),
                    rs.getString("pin"),
                    Money.of(rs.getBigDecimal("balance")),
                    Money.of(rs.getBigDecimal("daily_limit"))
                );
                loadTransactions(account);
                accounts.add(account);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todas as contas.", e);
        }

        return accounts;
    }

    @Override
    public void delete(Account account) {
        if (account != null) {
            deleteById(account.getId());
        }
    }

    @Override
    public void deleteById(UUID id) {
        if (id == null) {
            return;
        }

        String deleteTxSql = "DELETE FROM tb_transaction WHERE account_id = ?;";
        String deleteAccountSql = "DELETE FROM tb_account WHERE id = ?;";

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmtTx = conn.prepareStatement(deleteTxSql)) {
                    stmtTx.setString(1, id.toString());
                    stmtTx.executeUpdate();
                }

                try (PreparedStatement stmtAccount = conn.prepareStatement(deleteAccountSql)) {
                    stmtAccount.setString(1, id.toString());
                    stmtAccount.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Erro ao deletar conta por ID: " + id, e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha de conexão ao deletar conta: " + id, e);
        }
    }

    private void loadTransactions(Account account) {
        String sql = """
            SELECT id, account_id, type, amount, created_at, description
            FROM tb_transaction
            WHERE account_id = ?
            ORDER BY created_at ASC;
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, account.getId().toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID txId = UUID.fromString(rs.getString("id"));
                    TransactionType type = TransactionType.valueOf(rs.getString("type"));
                    BigDecimal amount = rs.getBigDecimal("amount");
                    LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
                    String description = rs.getString("description");

                    Transaction tx = new Transaction(txId, createdAt, type, Money.of(amount), description);
                    account.seedTransaction(tx);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao carregar transações da conta: " + account.getAccountNumber(), e);
        }
    }
}
