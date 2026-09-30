package com.inventory.gui;

import javax.swing.*;

public class SwingApp {
    public static void main(String[] args) {
        // Set System Look and Feel for native, crisp OS UI rendering
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
