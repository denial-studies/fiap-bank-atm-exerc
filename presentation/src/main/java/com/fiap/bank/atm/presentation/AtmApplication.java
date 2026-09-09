package com.fiap.bank.atm.presentation;

import com.fiap.bank.atm.application.service.AtmService;
import javax.swing.SwingUtilities;

public class AtmApplication {
    public static void main(String[] args) {
        // Inicializa a camada de Aplicação de forma desacoplada (DDD)
        AtmService atmService = new AtmService();

        // Inicializa a camada de Apresentação de forma segura na Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            AtmFrame mainFrame = new AtmFrame(atmService);
            mainFrame.setVisible(true);
        });
    }
}
