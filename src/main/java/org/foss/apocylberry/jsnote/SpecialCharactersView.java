package org.foss.apocylberry.jsnote;

import javax.swing.text.*;
import java.awt.*;

public class SpecialCharactersView extends PlainView {
    private boolean showSpecialCharacters;

    public SpecialCharactersView(Element elem) {
        super(elem);
    }

    @Override
    public float getPreferredSpan(int axis) {
        return super.getPreferredSpan(axis);
    }

    @Override
    public void paint(Graphics g, Shape allocation) {
        super.paint(g, allocation);
        
        if (!showSpecialCharacters) return;
        
        try {
            // Get the text content
            Document doc = getDocument();
            int startOffset = getStartOffset();
            int endOffset = getEndOffset();
            String text = doc.getText(startOffset, endOffset - startOffset);
            
            // Convert graphics context for proper rendering
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Save original color
            Color originalColor = g2d.getColor();
            g2d.setColor(new Color(128, 128, 128, 180)); // Semi-transparent gray
            
            Rectangle alloc = allocation instanceof Rectangle ? 
                (Rectangle) allocation : allocation.getBounds();
            
            // Get font metrics for positioning
            FontMetrics fm = g2d.getFontMetrics();
            int baseline = alloc.y + alloc.height - fm.getDescent();
            
            int x = alloc.x;
            
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                int charWidth = fm.charWidth(c);
                
                switch (c) {
                    case ' ':
                        // Draw middle dot for space
                        int dotX = x + charWidth/2;
                        int dotY = baseline - fm.getAscent()/2;
                        g2d.fillOval(dotX-1, dotY-1, 2, 2);
                        break;
                    case '\t':
                        // Draw right arrow for tab
                        int arrowY = baseline - fm.getAscent()/2;
                        g2d.drawLine(x, arrowY, x + charWidth - 2, arrowY);
                        g2d.drawLine(x + charWidth - 6, arrowY - 3, x + charWidth - 2, arrowY);
                        g2d.drawLine(x + charWidth - 6, arrowY + 3, x + charWidth - 2, arrowY);
                        break;
                    case '\n':
                        // Draw return symbol for newline
                        int returnY = baseline - fm.getAscent()/2;
                        g2d.drawLine(x + 2, returnY, x + charWidth - 2, returnY);
                        g2d.drawLine(x + 2, returnY, x + 2, returnY + 4);
                        g2d.drawLine(x, returnY + 4, x + 4, returnY + 4);
                        break;
                }
                
                x += charWidth;
            }
            
            // Restore original color
            g2d.setColor(originalColor);
            
        } catch (BadLocationException e) {
            // Ignore painting errors
        }
    }

    public void setShowSpecialCharacters(boolean show) {
        this.showSpecialCharacters = show;
    }
}
