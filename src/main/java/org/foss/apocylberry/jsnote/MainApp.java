package org.foss.apocylberry.jsnote;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.prefs.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class MainApp extends JFrame {
    private EditorPane editor;
    private JLabel statusBar;
    private JLabel messageArea;
    private JLabel cursorPos;
    private File currentFile = null;
    private Preferences prefs;
    private String dateFormat;
    private TimestampFormatManager timestampManager;
    private JMenu timestampFormatMenu;
    private FindReplaceDialog findDialog;
    private FindReplaceDialog replaceDialog;
    private boolean showSpecialCharacters = false;
    private LineNumberView lineNumberView;
    private JScrollPane scrollPane;
    
    private void toggleLineNumbers(boolean show) {
        lineNumberView.setVisible(show);
        prefs.putBoolean("showLineNumbers", show);
    }

    public MainApp() {
        prefs = Preferences.userNodeForPackage(MainApp.class);
        timestampManager = new TimestampFormatManager(prefs);
        dateFormat = prefs.get("dateFormat", timestampManager.getDefaultFormat());
        initComponents();
    }
    
    private void insertDateTime() {
        try {
            // Split the format into parts by escape sequences
            String[] parts = dateFormat.split("\\\\[ntr]");
            StringBuilder result = new StringBuilder();
            
            // Find all positions of escape sequences
            List<Integer> escapePositions = new ArrayList<>();
            List<Character> escapeChars = new ArrayList<>();
            for (int i = 0; i < dateFormat.length() - 1; i++) {
                if (dateFormat.charAt(i) == '\\') {
                    char next = dateFormat.charAt(i + 1);
                    if (next == 'n' || next == 't' || next == 'r') {
                        escapePositions.add(i);
                        escapeChars.add(next);
                    }
                }
            }
            
            // Process each part and add escape sequences between them
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    // Format this part of the date/time
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(parts[i].trim());
                    result.append(LocalDateTime.now().format(formatter));
                }
                
                // Add escape sequence if there is one
                if (i < escapeChars.size()) {
                    char escapeChar = escapeChars.get(i);
                    switch (escapeChar) {
                        case 'n': result.append('\n'); break;
                        case 't': result.append('\t'); break;
                        case 'r': result.append('\r'); break;
                    }
                }
            }
            
            editor.replaceSelection(result.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error formatting date/time: " + ex.getMessage(),
                "Format Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateTimestampFormatMenu() {
        timestampFormatMenu.removeAll();
        
        // Add recent formats
        for (String format : timestampManager.getRecentFormats()) {
            JMenuItem item = new JMenuItem(TimestampFormatManager.displayEscapeSequences(format));
            item.addActionListener(e -> {
                dateFormat = format;
                prefs.put("dateFormat", format);
            });
            timestampFormatMenu.add(item);
        }
        
        if (!timestampManager.getRecentFormats().isEmpty()) {
            timestampFormatMenu.addSeparator();
        }
        
        // Add "New Format..." option
        addMenuItem(timestampFormatMenu, "New Format...", null, e -> configureDateFormat());
        
        // Add "Clear Custom Formats" option
        addMenuItem(timestampFormatMenu, "Clear Custom Formats", null, e -> {
            timestampManager.clearFormats();
            dateFormat = timestampManager.getDefaultFormat();
            prefs.put("dateFormat", dateFormat);
            updateTimestampFormatMenu();
        });
    }
    
    private void configureDateFormat() {
        DateFormatDialog dialog = new DateFormatDialog(this, dateFormat);
        if (dialog.showDialog()) {
            dateFormat = dialog.getSelectedFormat();
            prefs.put("dateFormat", dateFormat);
            timestampManager.addFormat(dateFormat);
            updateTimestampFormatMenu();
        }
    }
    
    private void toggleSpecialCharacters(boolean show) {
        showSpecialCharacters = show;
        prefs.putBoolean("showSpecialChars", show);
        SpecialCharactersEditorKit.setShowSpecialCharacters(show);
        editor.repaint();
    }

    private void showFindDialog() {
        if (findDialog == null) {
            findDialog = new FindReplaceDialog(this, editor, false);
        }
        findDialog.showDialog();
    }

    private void showReplaceDialog() {
        if (replaceDialog == null) {
            replaceDialog = new FindReplaceDialog(this, editor, true);
        }
        replaceDialog.showDialog();
    }

    private void initComponents() {
        setTitle("Swing Note");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Create editor
        editor = new EditorPane();
        editor.setMaxLineLength(prefs.getInt("maxLineLength", 1024));
        
        // Add Replace (Ctrl+H) binding
        KeyStroke replaceKey = KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK);
        editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(replaceKey, "showReplace");
        editor.getActionMap().put("showReplace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showReplaceDialog();
            }
        });
        
        // Set application icon
        setIconImage(createApplicationIcon());
        
        // Add F5 key binding for date/time stamp
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "insertDateTime");
        editor.getActionMap().put("insertDateTime", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertDateTime();
            }
        });
        
        // Create scroll pane with line numbers
        lineNumberView = new LineNumberView(editor);
        scrollPane = new JScrollPane(editor);
        scrollPane.setRowHeaderView(lineNumberView);
        scrollPane.setVisible(true);
        toggleLineNumbers(prefs.getBoolean("showLineNumbers", false));
        add(scrollPane, BorderLayout.CENTER);
        
        // Create status bar with separate message area
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        
        messageArea = new JLabel(" ");
        statusBar = new JLabel(" ");
        
        JPanel leftStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftStatus.setOpaque(false);
        cursorPos = new JLabel("");
        leftStatus.add(cursorPos);
        
        JPanel rightStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightStatus.setOpaque(false);
        rightStatus.add(statusBar);
        
        messageArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        messageArea.setHorizontalAlignment(SwingConstants.LEFT);
        
        statusPanel.add(leftStatus, BorderLayout.WEST);
        statusPanel.add(messageArea, BorderLayout.CENTER);
        statusPanel.add(rightStatus, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);
        
        // Update cursor position in status bar
        editor.addCaretListener(e -> updateStatus());
        
        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        addMenuItem(fileMenu, "New", KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), e -> newFile());
        addMenuItem(fileMenu, "Open...", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), e -> openFile());
        addMenuItem(fileMenu, "Save", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), e -> saveFile());
        addMenuItem(fileMenu, "Save As...", null, e -> saveFileAs());
        fileMenu.addSeparator();
        addMenuItem(fileMenu, "Exit", null, e -> System.exit(0));
        
        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        addMenuItem(editMenu, "Undo", KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), e -> {
            try {
                if (editor.getUndoManager().canUndo()) {
                    editor.getUndoManager().undo();
                }
            } catch (Exception ex) {
                // Ignore undo errors
            }
        });
        addMenuItem(editMenu, "Redo", KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), e -> {
            try {
                if (editor.getUndoManager().canRedo()) {
                    editor.getUndoManager().redo();
                }
            } catch (Exception ex) {
                // Ignore redo errors
            }
        });
        editMenu.addSeparator();
        addMenuItem(editMenu, "Cut", KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), e -> editor.cut());
        addMenuItem(editMenu, "Copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), e -> editor.copy());
        addMenuItem(editMenu, "Paste", KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), e -> editor.paste());
        editMenu.addSeparator();
        addMenuItem(editMenu, "Find...", KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), e -> showFindDialog());
        addMenuItem(editMenu, "Find Next", KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), e -> FindReplaceDialog.findNext(editor));
        addMenuItem(editMenu, "Find Previous", KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK), e -> FindReplaceDialog.findPrevious(editor));
        addMenuItem(editMenu, "Replace...", KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), e -> showReplaceDialog());
        // No need for duplicate key binding here since we handle it in initComponents()
        editMenu.addSeparator();
        addMenuItem(editMenu, "Select All", KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), e -> editor.selectAll());
        editMenu.addSeparator();
        JCheckBoxMenuItem showSpecialCharsItem = new JCheckBoxMenuItem("Show special characters");
        showSpecialCharsItem.setState(showSpecialCharacters);
        showSpecialCharsItem.addActionListener(e -> toggleSpecialCharacters(showSpecialCharsItem.isSelected()));
        editMenu.add(showSpecialCharsItem);
        
        // Format menu
        // Create toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);
        
        // View menu
        JMenu viewMenu = new JMenu("View");
        JCheckBoxMenuItem lineNumbersItem = new JCheckBoxMenuItem("Line Numbers");
        lineNumbersItem.setState(prefs.getBoolean("showLineNumbers", false));
        lineNumbersItem.addActionListener(e -> toggleLineNumbers(lineNumbersItem.isSelected()));
        viewMenu.add(lineNumbersItem);
        
        // Format menu
        JMenu formatMenu = new JMenu("Format");
        JCheckBoxMenuItem wordWrapItem = new JCheckBoxMenuItem("Word Wrap");
        wordWrapItem.setState(editor.getLineWrap());
        wordWrapItem.addActionListener(e -> toggleWordWrap(wordWrapItem.isSelected()));
        formatMenu.add(wordWrapItem);
        
        formatMenu.addSeparator();
        addMenuItem(formatMenu, "Font...", null, e -> chooseFont());
        addMenuItem(formatMenu, "Logical Record Length...", null, e -> setLineLength());
        formatMenu.addSeparator();
        
        // Create timestamp format submenu
        timestampFormatMenu = new JMenu("Timestamp Format (F5)");
        updateTimestampFormatMenu();
        formatMenu.add(timestampFormatMenu);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(formatMenu);
        setJMenuBar(menuBar);
        
        // Set initial size and position
        setPreferredSize(new Dimension(800, 600));
        pack();
        setLocationRelativeTo(null);
        
        updateStatus();
    }
    
    private void addMenuItem(JMenu menu, String label, KeyStroke accelerator, ActionListener action) {
        JMenuItem item = new JMenuItem(label);
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        item.addActionListener(action);
        menu.add(item);
    }
    
    public void setStatusMessage(String message) {
        if (message != null && !message.isEmpty()) {
            messageArea.setText(message);
        } else {
            messageArea.setText(" ");
        }
    }

    private void updateStatus() {
        cursorPos.setText(editor.getCursorPosition());
        String lrecl = editor.getMaxLineLength() > 0 ? 
            String.format("LRECL: %d", editor.getMaxLineLength()) :
            "LRECL: none";
        statusBar.setText(lrecl);
    }
    
    private void newFile() {
        if (checkUnsavedChanges()) {
            editor.setText("");
            currentFile = null;
            setTitle("Swing Note");
        }
    }
    
    private void openFile() {
        if (!checkUnsavedChanges()) {
            return;
        }
        
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openFile(chooser.getSelectedFile());
        }
    }
    

    private void openFile(File file) {
        try {
            String content = Files.readString(file.toPath());
            editor.setText(content);
            currentFile = file;
            setTitle("Swing Note - " + currentFile.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveFile() {
        if (currentFile == null) {
            saveFileAs();
        } else {
            saveToFile(currentFile);
        }
    }
    
    private void saveFileAs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
            }
            public String getDescription() {
                return "Text Files (*.txt)";
            }
        });
        chooser.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || !f.getName().toLowerCase().endsWith(".txt");
            }
            public String getDescription() {
                return "All Files (*.*)";
            }
        });
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            
            // Add .txt extension if not specified and txt filter is selected
            if (chooser.getFileFilter().getDescription().startsWith("Text Files") && 
                !file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getPath() + ".txt");
            }
            
            if (file.exists()) {
                int result = JOptionPane.showConfirmDialog(this,
                    "File exists. Overwrite?", "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            saveToFile(file);
        }
    }
    
    private void saveToFile(File file) {
        try {
            Files.writeString(file.toPath(), editor.getText());
            currentFile = file;
            setTitle("Swing Note - " + currentFile.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private boolean checkUnsavedChanges() {
        if (editor.getText().isEmpty()) {
            return true;
        }
        
        int result = JOptionPane.showConfirmDialog(this,
            "Do you want to save changes?", "Save Changes",
            JOptionPane.YES_NO_CANCEL_OPTION);
            
        if (result == JOptionPane.CANCEL_OPTION) {
            return false;
        } else if (result == JOptionPane.YES_OPTION) {
            saveFile();
        }
        
        return true;
    }
    
    private void toggleWordWrap(boolean enable) {
        editor.setLineWrap(enable);
        editor.setWrapStyleWord(enable);
        editor.revalidate();
        editor.repaint();
        editor.getDocument().putProperty(PlainDocument.tabSizeAttribute, 4);
        editor.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
    }
    
    private void chooseFont() {
        Font currentFont = editor.getFont();
        JFontChooser fontChooser = new JFontChooser(currentFont);
        if (fontChooser.showDialog(this) == JFontChooser.OK_OPTION) {
            Font newFont = fontChooser.getSelectedFont();
            // Ensure we keep monospace
            if (isMonospaced(newFont)) {
                editor.setFont(newFont);
                prefs.put("fontFamily", newFont.getFamily());
                prefs.putInt("fontSize", newFont.getSize());
                prefs.putInt("fontStyle", newFont.getStyle());
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Only monospaced fonts are allowed.", 
                    "Font Selection", 
                    JOptionPane.WARNING_MESSAGE);
            }
        }
    }
    
    private boolean isMonospaced(Font font) {
        FontMetrics metrics = getFontMetrics(font);
        int width1 = metrics.charWidth('i');
        int width2 = metrics.charWidth('W');
        return width1 == width2;
    }
    
    private Image createApplicationIcon() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw a background square with rounded corners
        g2d.setColor(new Color(103, 58, 183)); // Material Design Purple
        g2d.fillRoundRect(2, 2, 28, 28, 6, 6);
        
        // Draw a text note symbol
        g2d.setColor(new Color(237, 231, 246)); // Light purple
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.drawString("♪", 10, 23);
        
        g2d.dispose();
        return img;
    }
    
    private void setLineLength() {
        String result = JOptionPane.showInputDialog(this,
            "Enter maximum line length (0 for no limit):",
            editor.getMaxLineLength());
            
        if (result != null) {
            try {
                int length = Integer.parseInt(result);
                if (length >= 0) {
                    editor.setMaxLineLength(length);
                    prefs.putInt("maxLineLength", length);
                    updateStatus();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Please enter a non-negative number.",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                    "Please enter a valid number.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            MainApp app = new MainApp();
            app.setVisible(true);
            
            // If a file path was provided as command line argument, open it
            if (args.length > 0) {
                File fileToOpen = new File(args[0]);
                if (fileToOpen.exists() && fileToOpen.isFile()) {
                    app.openFile(fileToOpen);
                }
            }
        });
    }
}
