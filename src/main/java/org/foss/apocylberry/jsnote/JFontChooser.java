package org.foss.apocylberry.jsnote;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class JFontChooser extends JDialog {
    public static final int OK_OPTION = JOptionPane.OK_OPTION;
    public static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
    
    private JList<String> fontNameList;
    private JList<Integer> fontSizeList;
    private JCheckBox boldCheckBox;
    private JCheckBox italicCheckBox;
    private JTextArea previewArea;
    private int result = CANCEL_OPTION;
    private Font selectedFont;
    
    public JFontChooser(Font initialFont) {
        super((Frame)null, "Select Font", true);
        selectedFont = initialFont;
        initComponents();
        updatePreview();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Create lists panel
        JPanel listsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        
        // Font names (only monospaced fonts)
        DefaultListModel<String> fontModel = new DefaultListModel<>();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (Font font : ge.getAllFonts()) {
            if (isMonospaced(font)) {
                fontModel.addElement(font.getFamily());
            }
        }
        fontNameList = new JList<>(fontModel);
        fontNameList.setSelectedValue(selectedFont.getFamily(), true);
        fontNameList.addListSelectionListener(e -> updatePreview());
        
        // Font sizes
        DefaultListModel<Integer> sizeModel = new DefaultListModel<>();
        int[] sizes = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72};
        for (int size : sizes) {
            sizeModel.addElement(size);
        }
        fontSizeList = new JList<>(sizeModel);
        fontSizeList.setSelectedValue(selectedFont.getSize(), true);
        fontSizeList.addListSelectionListener(e -> updatePreview());
        
        listsPanel.add(new JScrollPane(fontNameList));
        listsPanel.add(new JScrollPane(fontSizeList));
        
        // Style checkboxes
        JPanel stylePanel = new JPanel();
        boldCheckBox = new JCheckBox("Bold");
        italicCheckBox = new JCheckBox("Italic");
        boldCheckBox.setSelected((selectedFont.getStyle() & Font.BOLD) != 0);
        italicCheckBox.setSelected((selectedFont.getStyle() & Font.ITALIC) != 0);
        
        boldCheckBox.addActionListener(e -> updatePreview());
        italicCheckBox.addActionListener(e -> updatePreview());
        
        stylePanel.add(boldCheckBox);
        stylePanel.add(italicCheckBox);
        
        // Preview
        previewArea = new JTextArea("AaBbCc123");
        previewArea.setEditable(false);
        previewArea.setPreferredSize(new Dimension(300, 100));
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            result = OK_OPTION;
            dispose();
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        // Layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(listsPanel, BorderLayout.CENTER);
        mainPanel.add(stylePanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(previewArea), BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
    }
    
    private void updatePreview() {
        if (fontNameList.getSelectedValue() != null && fontSizeList.getSelectedValue() != null) {
            int style = Font.PLAIN;
            if (boldCheckBox.isSelected()) style |= Font.BOLD;
            if (italicCheckBox.isSelected()) style |= Font.ITALIC;
            
            selectedFont = new Font(fontNameList.getSelectedValue(), 
                                 style, 
                                 fontSizeList.getSelectedValue());
            previewArea.setFont(selectedFont);
        }
    }
    
    private boolean isMonospaced(Font font) {
        FontMetrics metrics = getFontMetrics(font);
        int width1 = metrics.charWidth('i');
        int width2 = metrics.charWidth('W');
        return width1 == width2;
    }
    
    public int showDialog(Component parent) {
        setLocationRelativeTo(parent);
        setVisible(true);
        return result;
    }
    
    public Font getSelectedFont() {
        return selectedFont;
    }
}
