package com.garmentscafeteria.ui;

import com.garmentscafeteria.db.DatabaseManager;
import javax.swing.*;
import java.awt.*;

public class ReportingFrame extends JFrame {
    public ReportingFrame() {
        setTitle("Sales Reports");
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTextArea reportArea = new JTextArea();
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        reportArea.setEditable(false);
        
        // Fetch and display report
        String reportText = DatabaseManager.getDailySalesReport();
        reportArea.setText(reportText);
        
        add(new JScrollPane(reportArea));
    }
}