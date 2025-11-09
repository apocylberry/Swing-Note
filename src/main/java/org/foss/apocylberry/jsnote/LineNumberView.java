package org.foss.apocylberry.jsnote;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class LineNumberView extends JPanel {
    private EditorPane editor;
    private Map<String, FontMetrics> cachedMetrics;
    private int lastHeight;
    private static final int MARGIN = 5;
    
    public LineNumberView(EditorPane editor) {
        this.editor = editor;
        this.cachedMetrics = new HashMap<>();
        setPreferredWidth();
        setBackground(new Color(240, 240, 240));
        
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { repaint(); }
            public void removeUpdate(DocumentEvent e) { repaint(); }
            public void changedUpdate(DocumentEvent e) { repaint(); }
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
    
    private int getLineNumberAtPoint(int y) {
        Element root = editor.getDocument().getDefaultRootElement();
        int pos = editor.viewToModel2D(new Point(0, y));
        return root.getElementIndex(pos) + 1;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        Rectangle clip = g2d.getClipBounds();
        int lineHeight = editor.getLineHeight();
        int startLine = getLineNumberAtPoint(clip.y);
        int endLine = getLineNumberAtPoint(clip.y + clip.height);
        
        int width = getWidth();
        Font font = editor.getFont().deriveFont(Font.PLAIN, editor.getFont().getSize() - 1f);
        g2d.setFont(font);
        g2d.setColor(new Color(100, 100, 100));
        
        Element root = editor.getDocument().getDefaultRootElement();
        int lastLineStart = -1;
        
        for (int i = startLine; i <= endLine; i++) {
            String text = String.valueOf(i);
            int stringWidth = g2d.getFontMetrics().stringWidth(text);
            int x = width - stringWidth - MARGIN;
            
            try {
                Element elem = root.getElement(i-1);
                int startOffset = elem.getStartOffset();
                
                // Only draw line number if this is not a wrapped continuation
                Rectangle r = editor.modelToView2D(startOffset).getBounds();
                if (startOffset > lastLineStart) {
                    g2d.drawString(text, x, r.y + r.height - 2);
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
