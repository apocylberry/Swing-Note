package org.foss.apocylberry.jsnote;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class FindReplaceDialog extends JDialog {
    private JTextField findField;
    private JTextField replaceField;
    private JButton findNextButton;
    private JButton findPrevButton;
    private JButton replaceButton;
    private JButton replaceAllButton;
    private JCheckBox matchCaseCheckBox;
    private EditorPane editor;
    private boolean isReplace;

    private static String lastSearchTerm = "";
    private static boolean lastMatchCase = false;
    private static boolean wrapSearch = true;

    public FindReplaceDialog(Frame owner, EditorPane editor, boolean isReplace) {
        super(owner, isReplace ? "Replace" : "Find", false);
        this.editor = editor;
        this.isReplace = isReplace;
        
        // Register document listeners for undo support
        editor.getDocument().addUndoableEditListener(editor.getUndoManager());
        
        initComponents();

        // Handle Escape key
        KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(e -> setVisible(false),
                                          escapeStroke,
                                          JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Handle F3 and Shift+F3 in dialog
        KeyStroke f3Stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
        KeyStroke shiftF3Stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK);
        
        getRootPane().registerKeyboardAction(e -> findNext(),
                                          f3Stroke,
                                          JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> findPrevious(),
                                          shiftF3Stroke,
                                          JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        // Find field
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Find:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        findField = new JTextField(20);
        findField.addActionListener(e -> findNext());
        mainPanel.add(findField, gbc);

        // Replace field (only if in replace mode)
        if (isReplace) {
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0.0;
            mainPanel.add(new JLabel("Replace with:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            replaceField = new JTextField(20);
            replaceField.addActionListener(e -> replace());
            mainPanel.add(replaceField, gbc);
        }

        // Options panel
        gbc.gridx = 0;
        gbc.gridy = isReplace ? 2 : 1;
        gbc.gridwidth = 2;
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        matchCaseCheckBox = new JCheckBox("Match case");
        JCheckBox wrapSearchCheckBox = new JCheckBox("Search past end", wrapSearch);
        wrapSearchCheckBox.addActionListener(e -> wrapSearch = wrapSearchCheckBox.isSelected());
        optionsPanel.add(matchCaseCheckBox);
        optionsPanel.add(wrapSearchCheckBox);
        mainPanel.add(optionsPanel, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        findNextButton = new JButton("Find Next");
        findPrevButton = new JButton("Find Previous");
        findNextButton.addActionListener(e -> findNext());
        findPrevButton.addActionListener(e -> findPrevious());
        buttonPanel.add(findNextButton);
        buttonPanel.add(findPrevButton);

        if (isReplace) {
            replaceButton = new JButton("Replace");
            replaceAllButton = new JButton("Replace All");
            replaceButton.addActionListener(e -> replace());
            replaceAllButton.addActionListener(e -> replaceAll());
            buttonPanel.add(replaceButton);
            buttonPanel.add(replaceAllButton);

            // Add Alt+A shortcut for Replace All
            replaceAllButton.setMnemonic(KeyEvent.VK_A);
        }

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Handle window closing
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        
        // Set initial size and make it non-resizable
        pack();
        setResizable(false);

        // Add document listener to enable/disable buttons based on find field content
        findField.getDocument().addDocumentListener(new DocumentListener() {
            private void updateButtons() {
                boolean hasText = findField.getText().length() > 0;
                findNextButton.setEnabled(hasText);
                findPrevButton.setEnabled(hasText);
                if (isReplace) {
                    replaceButton.setEnabled(hasText);
                    replaceAllButton.setEnabled(hasText);
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) { updateButtons(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateButtons(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateButtons(); }
        });

        // Initially disable buttons until there's search text
        findNextButton.setEnabled(false);
        findPrevButton.setEnabled(false);
        if (isReplace) {
            replaceButton.setEnabled(false);
            replaceAllButton.setEnabled(false);
        }
    }

    public void showDialog() {
        // Set location relative to parent window
        setLocationRelativeTo(getOwner());
        
        // Use last search term if available, otherwise use selected text
        String selectedText = editor.getSelectedText();
        if (lastSearchTerm != null && !lastSearchTerm.isEmpty()) {
            findField.setText(lastSearchTerm);
        } else if (selectedText != null && !selectedText.isEmpty()) {
            findField.setText(selectedText);
        }
        matchCaseCheckBox.setSelected(lastMatchCase);
        
        // Show dialog and focus find field
        setVisible(true);
        findField.requestFocusInWindow();
        findField.selectAll();
    }

    public static void findNext(EditorPane editor) {
        if (lastSearchTerm != null && !lastSearchTerm.isEmpty()) {
            searchText(editor, lastSearchTerm, lastMatchCase, true);
        }
    }

    public static void findPrevious(EditorPane editor) {
        if (lastSearchTerm != null && !lastSearchTerm.isEmpty()) {
            searchText(editor, lastSearchTerm, lastMatchCase, false);
        }
    }

    private static void searchText(EditorPane editor, String searchTerm, boolean matchCase, boolean forward) {
        String originalText = editor.getText();
        String text = matchCase ? originalText : originalText.toLowerCase();
        String searchFor = matchCase ? searchTerm : searchTerm.toLowerCase();
        
        int position = editor.getCaretPosition();
        if (!forward) {
            position = editor.getSelectionStart();
        }
        
        int foundIndex;
        if (forward) {
            foundIndex = text.indexOf(searchFor, position);
            if (foundIndex == -1 && wrapSearch && position > 0) {
                // Wrap around to start
                foundIndex = text.indexOf(searchFor, 0);
                if (foundIndex != -1) {
                    updateStatus(editor, "Search wrapped to beginning of document");
                }
            } else if (foundIndex == -1 && !wrapSearch) {
                updateStatus(editor, "Reached end of document");
            }
        } else {
            foundIndex = text.lastIndexOf(searchFor, position - 1);
            if (foundIndex == -1 && wrapSearch && position < text.length()) {
                // Wrap around to end
                foundIndex = text.lastIndexOf(searchFor);
                if (foundIndex != -1) {
                    updateStatus(editor, "Search wrapped to end of document");
                }
            } else if (foundIndex == -1 && !wrapSearch) {
                updateStatus(editor, "Reached beginning of document");
            }
        }

        if (foundIndex != -1) {
            // Clear any previous status messages
            updateStatus(editor, "");
            // Update selection - make sure to select the exact match
            editor.setCaretPosition(foundIndex);
            editor.moveCaretPosition(foundIndex + searchFor.length());
            editor.getCaret().setSelectionVisible(true);
            // Make sure the highlighted text is visible
            try {
                Rectangle viewRect = editor.modelToView2D(foundIndex).getBounds();
                editor.scrollRectToVisible(viewRect);
            } catch (BadLocationException ex) {
                // Ignore if we can't scroll
            }
        } else if (searchFor.length() > 0) {
            updateStatus(editor, "Cannot find \"" + searchTerm + "\"");
        }
    }
    
    private static void updateStatus(EditorPane editor, String message) {
        MainApp mainApp = (MainApp) SwingUtilities.getWindowAncestor(editor);
        if (mainApp != null) {
            mainApp.setStatusMessage(message);
        }
    }

    private void findNext() {
        find(true);
    }

    private void findPrevious() {
        find(false);
    }

    private void find(boolean forward) {
        String searchTerm = findField.getText();
        lastSearchTerm = searchTerm;
        lastMatchCase = matchCaseCheckBox.isSelected();
        searchText(editor, searchTerm, lastMatchCase, forward);
    }

    private void replace() {
        String selectedText = editor.getSelectedText();
        String searchTerm = findField.getText();
        if (selectedText != null && 
            (matchCaseCheckBox.isSelected() ? 
                selectedText.equals(searchTerm) : 
                selectedText.equalsIgnoreCase(searchTerm))) {
            editor.replaceSelection(replaceField.getText());
        }
        findNext();
    }

    private void replaceAll() {
        String searchTerm = findField.getText();
        String replacement = replaceField.getText();
        String text = editor.getText();
        boolean matchCase = matchCaseCheckBox.isSelected();

        int count = 0;
        try {
            // Start with a new UndoableEdit group
            CompoundEdit compoundEdit = new CompoundEdit();
            Document doc = editor.getDocument();

            int lastIndex = 0;
            while (true) {
                String searchIn = matchCase ? text : text.toLowerCase();
                String searchFor = matchCase ? searchTerm : searchTerm.toLowerCase();
                int foundIndex = searchIn.indexOf(searchFor, lastIndex);
                
                if (foundIndex == -1) break;
                
                // Verify this is a whole word match
                boolean validMatch = true;
                if (foundIndex > 0) {
                    char before = text.charAt(foundIndex - 1);
                    if (Character.isLetterOrDigit(before)) {
                        validMatch = false;
                    }
                }
                if (foundIndex + searchTerm.length() < text.length()) {
                    char after = text.charAt(foundIndex + searchTerm.length());
                    if (Character.isLetterOrDigit(after)) {
                        validMatch = false;
                    }
                }
                
                if (validMatch) {
                    // Remove the old text and insert the new text
                    try {
                        doc.remove(foundIndex, searchTerm.length());
                        doc.insertString(foundIndex, replacement, null);
                        count++;
                        // Get the updated text after replacement
                        text = editor.getText();
                        lastIndex = foundIndex + replacement.length();
                    } catch (BadLocationException e) {
                        // Skip this replacement if it fails
                        lastIndex = foundIndex + 1;
                    }
                } else {
                    lastIndex = foundIndex + 1;
                }
            }
            
            // End the compound edit
            compoundEdit.end();
            editor.getUndoManager().addEdit(compoundEdit);

            if (count > 0) {
                JOptionPane.showMessageDialog(this,
                    count + " replacement(s) made",
                    "Replace All",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error during replace: " + ex.getMessage(),
                "Replace Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    }
