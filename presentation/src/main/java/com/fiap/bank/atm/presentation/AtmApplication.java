package com.fiap.bank.atm.presentation;

import com.fiap.bank.atm.domain.repository.AccountRepository;
import com.fiap.bank.atm.infrastructure.persistence.InMemoryAccountRepository;
import com.fiap.bank.atm.application.service.AtmService;
import javax.swing.SwingUtilities;

public class AtmApplication {
    public static void main(String[] args) {
        // Inicializa as camadas de Infraestrutura e Aplicação (DDD)
        AccountRepository accountRepository = new InMemoryAccountRepository();
        AtmService atmService = new AtmService(accountRepository);

        // Inicializa a camada de Apresentação de forma segura na Event Dispatch Thread
        // (EDT)
        SwingUtilities.invokeLater(() -> {
            AtmFrame mainFrame = new AtmFrame(atmService);
            mainFrame.setVisible(true);
        });
    }
}
