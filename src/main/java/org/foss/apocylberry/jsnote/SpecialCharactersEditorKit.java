package org.foss.apocylberry.jsnote;

import javax.swing.text.*;
import java.awt.*;

public class SpecialCharactersEditorKit extends StyledEditorKit {
    private boolean showSpecialCharacters = false;
    
    public void setShowSpecialCharacters(boolean show) {
        this.showSpecialCharacters = show;
    }
    
    public boolean isShowSpecialCharacters() {
        return showSpecialCharacters;
    }
    
    @Override
    public ViewFactory getViewFactory() {
        return new ViewFactory() {
            public View create(Element elem) {
                String kind = elem.getName();
                if (kind != null) {
                    switch (kind) {
                        case AbstractDocument.ContentElementName:
                            return new SpecialCharactersGlyphView(elem, showSpecialCharacters);
                        case AbstractDocument.ParagraphElementName:
                            return new SpecialCharactersParagraphView(elem, showSpecialCharacters);
                        case AbstractDocument.SectionElementName:
                            return new BoxView(elem, View.Y_AXIS);
                        case StyleConstants.ComponentElementName:
                            return new ComponentView(elem);
                        case StyleConstants.IconElementName:
                            return new IconView(elem);
                    }
                }
                return new LabelView(elem);
            }
        };
    }
    
    /**
     * GlyphView that renders special characters when enabled
     */
    private static class SpecialCharactersGlyphView extends GlyphView {
        private boolean showSpecialCharacters;
        
        public SpecialCharactersGlyphView(Element elem, boolean show) {
            super(elem);
            this.showSpecialCharacters = show;
        }
        
        public void setShowSpecialCharacters(boolean show) {
            this.showSpecialCharacters = show;
        }
        
        @Override
        public void paint(Graphics g, Shape allocation) {
            super.paint(g, allocation);
            
            if (showSpecialCharacters) {
                paintSpecialCharacters(g, allocation);
            }
        }
        
        private void paintSpecialCharacters(Graphics g, Shape allocation) {
            try {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Document doc = getDocument();
                int startOffset = getStartOffset();
                int endOffset = getEndOffset();
                int docLength = doc.getLength();
                
                String text = doc.getText(startOffset, endOffset - startOffset);
                
                Color originalColor = g2d.getColor();
                g2d.setColor(new Color(128, 128, 128, 180)); // Semi-transparent gray
                
                // Get container for position calculations
                Container container = getContainer();
                if (!(container instanceof javax.swing.text.JTextComponent)) {
                    g2d.setColor(originalColor);
                    return;
                }
                
                javax.swing.text.JTextComponent textComponent = (javax.swing.text.JTextComponent) container;
                
                // Process each character in this view
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    int offset = startOffset + i;
                    
                    // Skip trailing newline(s) at document end - for both \n and \r\n
                    // Check if this character and everything after it is only newlines
                    boolean isTrailingNewline = false;
                    if (c == '\n' || c == '\r') {
                        isTrailingNewline = true;
                        // Check if there's any non-newline character after this position
                        for (int check = offset + 1; check < docLength; check++) {
                            char nextChar = doc.getText(check, 1).charAt(0);
                            if (nextChar != '\n' && nextChar != '\r') {
                                isTrailingNewline = false;
                                break;
                            }
                        }
                    }
                    
                    if (isTrailingNewline) {
                        continue;
                    }
                    
                    // Get the visual position of this character
                    try {
                        @SuppressWarnings("deprecation")
                        java.awt.Rectangle charBounds = textComponent.modelToView(offset);
                        if (charBounds == null) {
                            continue;
                        }
                        
                        int charX = charBounds.x;
                        int charY = charBounds.y;
                        int charWidth = charBounds.width;
                        int charHeight = charBounds.height;
                        int centerX = charX + charWidth / 2;
                        int centerY = charY + charHeight / 2;
                        
                        switch (c) {
                            case ' ':
                                // Middle dot for space - truly centered in the space width
                                // Calculate position at the exact center of the space character
                                int spaceDotX = charX + charWidth / 2;
                                int spaceDotY = charY + charHeight / 2;
                                // Draw dot slightly larger to be more visible
                                g2d.fillOval(spaceDotX - 1, spaceDotY - 1, 2, 2);
                                break;
                            case '\t':
                                // Right arrow for tab - centered horizontally in the field
                                // The arrow should be in the middle of the tab width
                                int tabCenterX = charX + charWidth / 2;
                                // Arrow line pointing right through center
                                g2d.drawLine(tabCenterX - 3, centerY, tabCenterX + 3, centerY);
                                // Right-pointing arrowhead
                                g2d.drawLine(tabCenterX + 3, centerY, tabCenterX + 1, centerY - 2);
                                g2d.drawLine(tabCenterX + 3, centerY, tabCenterX + 1, centerY + 2);
                                break;
                            case '\n':
                                // Return arrow for line feed - DO NOT MODIFY
                                // Vertical line (4 units down)
                                g2d.drawLine(centerX, centerY - 3, centerX, centerY + 1);
                                // Horizontal bend to the left
                                g2d.drawLine(centerX, centerY + 1, centerX - 3, centerY + 1);
                                // Arrow head pointing left
                                g2d.drawLine(centerX - 3, centerY + 1, centerX - 1, centerY - 1);
                                g2d.drawLine(centerX - 3, centerY + 1, centerX - 1, centerY + 3);
                                break;
                            case '\r':
                                // Carriage return arrow (⏎ - left facing hook)
                                int crY = centerY;
                                int crX = centerX;
                                // Draw a left-pointing return hook
                                g2d.drawLine(crX + 2, crY - 2, crX - 2, crY - 2);
                                g2d.drawLine(crX - 2, crY - 2, crX - 2, crY + 2);
                                g2d.drawLine(crX - 2, crY + 2, crX, crY + 2);
                                g2d.drawLine(crX + 2, crY, crX - 2, crY);
                                break;
                        }
                    } catch (BadLocationException e) {
                        // If we can't get position for this character, skip it
                        continue;
                    }
                }
                
                g2d.setColor(originalColor);
                
            } catch (BadLocationException e) {
                // Ignore painting errors
            }
        }
    }
    
    /**
     * ParagraphView that properly renders special characters
     */
    private static class SpecialCharactersParagraphView extends ParagraphView {
        public SpecialCharactersParagraphView(Element elem, boolean show) {
            super(elem);
        }
        
        @Override
        public float getAlignment(int axis) {
            if (axis == X_AXIS) {
                return 0.0f;
            }
            return super.getAlignment(axis);
        }
    }
}
