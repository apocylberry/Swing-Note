package org.foss.apocylberry.jsnote;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.*;

public class LineNumberView extends JPanel {
    private EditorPane editor;
    private int lastHeight;
    private static final int MARGIN = 5;
    

    public LineNumberView(EditorPane editor) {
        this.editor = editor;
        setPreferredWidth();
        setBackground(new Color(240, 240, 240));
        
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { repaint(); }
            public void removeUpdate(DocumentEvent e) { repaint(); }
            public void changedUpdate(DocumentEvent e) { repaint(); }
        });
        
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
    }
    
    private void setPreferredWidth() {
        int lineCount = getLineCount();
        int digits = Math.max(3, String.valueOf(lineCount).length());
        int width = MARGIN * 2 + getFontMetrics(editor.getFont())
            .stringWidth("0".repeat(digits));
        setPreferredSize(new Dimension(width, 0));
    }
    

    private int getLineCount() {
        Element root = editor.getDocument().getDefaultRootElement();
        return root.getElementCount();
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
        
        // Get the visible rectangle of the editor to determine which lines are visible
        Rectangle visibleRect = editor.getVisibleRect();
        
        Element root = editor.getDocument().getDefaultRootElement();
        int startLine = root.getElementIndex(editor.viewToModel2D(new Point(0, visibleRect.y)));
        int endLine = root.getElementIndex(editor.viewToModel2D(new Point(0, visibleRect.y + visibleRect.height)));
        
        int lastLineStart = -1;
        
        // Draw line numbers for visible lines
        for (int i = startLine; i <= Math.min(endLine, root.getElementCount() - 1); i++) {
            try {
                Element elem = root.getElement(i);
                int startOffset = elem.getStartOffset();
                
                // Only draw line number if this is not a wrapped continuation
                if (startOffset > lastLineStart) {
                    Rectangle r = editor.modelToView2D(startOffset).getBounds();
                    
                    // Translate editor coordinates to line number view coordinates
                    int y = r.y - visibleRect.y + r.height - 2;
                    
                    String text = String.valueOf(i + 1);
                    int stringWidth = g2d.getFontMetrics().stringWidth(text);
                    int x = width - stringWidth - MARGIN;
                    
                    g2d.drawString(text, x, y);
                    lastLineStart = startOffset;
                }
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
