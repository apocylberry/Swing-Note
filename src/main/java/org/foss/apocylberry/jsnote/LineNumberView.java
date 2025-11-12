package org.foss.apocylberry.jsnote;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;


public class LineNumberView extends JPanel {
    private EditorPane editor;
    private int lastHeight;
    private static final int MARGIN = 5;
    private DocumentListener docListener;
    

    public LineNumberView(EditorPane editor) {
        this.editor = editor;
        setPreferredWidth();
        setBackground(new Color(240, 240, 240));
        
        // Create document listener once
        docListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { repaint(); }
            public void removeUpdate(DocumentEvent e) { repaint(); }
            public void changedUpdate(DocumentEvent e) { repaint(); }
        };
        
        // Attach to current document
        attachToDocument();
        
        // Add component listener to repaint when editor view changes
        editor.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                repaint();
            }
        });
        
        // Add caret listener to repaint when scrolling occurs
        editor.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                repaint();
            }
        });
        
        // Add property change listener to detect document changes
        editor.addPropertyChangeListener("document", evt -> {
            if (evt.getOldValue() != null) {
                ((Document)evt.getOldValue()).removeDocumentListener(docListener);
            }
            attachToDocument();
            repaint();
        });
    }
    
    private void attachToDocument() {
        editor.getDocument().addDocumentListener(docListener);
    }
    

    private void setPreferredWidth() {
        int lineCount = getLineCount();
        int digits = Math.max(3, String.valueOf(lineCount).length());
        int width = MARGIN * 2 + getFontMetrics(editor.getFont())
            .stringWidth("0".repeat(digits));
        setPreferredSize(new Dimension(width, editor.getPreferredSize().height));
    }
    

    private int getLineCount() {
        Element root = editor.getDocument().getDefaultRootElement();
        return root.getElementCount();
    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        // Match the editor's height so our coordinate system aligns
        d.height = editor.getPreferredSize().height;
        return d;
    }
    





    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        Rectangle clip = g2d.getClipBounds();
        int width = getWidth();
        Font font = editor.getFont().deriveFont(Font.PLAIN, editor.getFont().getSize() - 1f);
        g2d.setFont(font);
        g2d.setColor(new Color(100, 100, 100));
        
        FontMetrics fm = g2d.getFontMetrics();
        int fontHeight = fm.getHeight();
        
        Element root = editor.getDocument().getDefaultRootElement();
        int lineCount = root.getElementCount();
        
        // Draw line numbers for all lines in the document
        // Since our height matches the editor's height, we're in the same coordinate space
        for (int line = 0; line < lineCount; line++) {
            try {
                Element elem = root.getElement(line);
                int startOffset = elem.getStartOffset();
                
                // Get the rectangle for this line's start position
                Rectangle2D r2d = editor.modelToView2D(startOffset);
                if (r2d == null) continue;
                
                int y = (int) r2d.getY() + (int) r2d.getHeight() - 2;
                
                // Only draw if within the clip bounds (for performance)
                if (y < clip.y - fontHeight || y > clip.y + clip.height + fontHeight) {
                    continue;
                }
                
                // Draw the line number
                String text = String.valueOf(line + 1);
                int stringWidth = fm.stringWidth(text);
                int x = width - stringWidth - MARGIN;
                
                g2d.drawString(text, x, y);
                
            } catch (BadLocationException ex) {
                // Skip this line if we can't get its position
            }
        }
        
        // Draw separator line
        g2d.setColor(new Color(220, 220, 220));
        g2d.drawLine(width - 1, clip.y, width - 1, clip.y + clip.height);
        

        // Update width if line count changed significantly
        int currentHeight = editor.getHeight();
        if (currentHeight != lastHeight) {
            setPreferredWidth();
            lastHeight = currentHeight;
        }
    }
}
