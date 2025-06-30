package com.garmentscafeteria;

import com.formdev.flatlaf.FlatLightLaf;
import com.garmentscafeteria.db.DatabaseManager;
import com.garmentscafeteria.ui.LoginFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// THIS IS THE FIX: We've added the missing import for the exception.
import javax.swing.UnsupportedLookAndFeelException;

public class App {
    public static void main(String[] args) {
        // --- UI ENHANCEMENT ---
        // Set the modern FlatLaf look and feel before creating any UI components.
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            // This 'catch' block now works because the class is imported.
            System.err.println("Failed to initialize modern LaF. Using default.");
            e.printStackTrace();
        }

        // Initialize the database and create tables if they don't exist
        DatabaseManager.initializeDatabase();

        // Run the GUI on the Event Dispatch Thread (EDT) for thread safety
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}