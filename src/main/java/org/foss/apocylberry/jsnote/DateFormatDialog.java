package org.foss.apocylberry.jsnote;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class DateFormatDialog extends JDialog {
    private JTextField formatField;
    private JLabel previewLabel;
    private String selectedFormat;
    private boolean approved = false;
    
    public DateFormatDialog(Frame owner, String currentFormat) {
        super(owner, "Timestamp Format Settings", true);
        this.selectedFormat = currentFormat;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Format input
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("Format:"), gbc);
        
        formatField = new JTextField(selectedFormat, 30);
        formatField.addCaretListener(e -> updatePreview());
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        mainPanel.add(formatField, gbc);
        
        // Help text
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 0, 5, 0);
        mainPanel.add(new JLabel("<html>Common patterns:<br>" +
            "HH:mm M/d/yyyy - Hour:minute Month/day/year<br>" +
            "yyyy-MM-dd HH:mm:ss - ISO format<br>" +
            "MMMM d, yyyy h:mm a - Month day, year hour:minute AM/PM<br><br>" +
            "Special characters:<br>" +
            "\\n - New line (Example: HH:mm\\nM/d/yyyy)<br>" +
            "\\t - Tab (Example: HH:mm\\tM/d/yyyy)<br>" +
            "\\r - Carriage return</html>"), gbc);
        
        // Preview
        gbc.gridy = 2;
        gbc.insets = new Insets(10, 0, 10, 0);
        previewLabel = new JLabel();
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(previewLabel, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            approved = true;
            selectedFormat = formatField.getText();
            dispose();
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        updatePreview();
        pack();
        setLocationRelativeTo(getOwner());
    }
    
    private void updatePreview() {
        try {
            String format = formatField.getText();
            
            // Split by escape sequences
            String[] parts = format.split("\\\\[ntr]");
            StringBuilder preview = new StringBuilder();
            
            // Find all positions of escape sequences
            List<Integer> escapePositions = new ArrayList<>();
            List<Character> escapeChars = new ArrayList<>();
            for (int i = 0; i < format.length() - 1; i++) {
                if (format.charAt(i) == '\\') {
                    char next = format.charAt(i + 1);
                    if (next == 'n' || next == 't' || next == 'r') {
                        escapePositions.add(i);
                        escapeChars.add(next);
                    }
                }
            }
            
            // Process each part and add escape sequences between them
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(parts[i].trim());
                    preview.append(LocalDateTime.now().format(formatter));
                }
                
                // Add visible escape sequence if there is one
                if (i < escapeChars.size()) {
                    char escapeChar = escapeChars.get(i);
                    switch (escapeChar) {
                        case 'n': preview.append("↵\n"); break;
                        case 't': preview.append("→"); break;
                        case 'r': preview.append("⏎"); break;
                    }
                }
            }
            
            previewLabel.setText("Preview: " + preview.toString());
            previewLabel.setForeground(Color.BLACK);
        } catch (Exception e) {
            previewLabel.setText("Invalid format");
            previewLabel.setForeground(Color.RED);
        }
    }
    
    public boolean showDialog() {
        setVisible(true);
        return approved;
    }
    
    public String getSelectedFormat() {
        return selectedFormat;
    }
}
