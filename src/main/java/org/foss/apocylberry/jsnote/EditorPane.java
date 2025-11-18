package org.foss.apocylberry.jsnote;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.undo.*;

public class EditorPane extends JTextPane {

    /**
     * Custom UndoManager that properly preserves undo history when adding CompoundEdits.
     * The standard UndoManager clears the undo stack when a new edit is added, which
     * prevents access to edits prior to a CompoundEdit. This implementation preserves that history.
     */
    private static class HistoryPreservingUndoManager extends UndoManager {
        private int trackedLimit = -1;
        
        @Override
        public synchronized void setLimit(int l) {
            trackedLimit = l;
            super.setLimit(l);
        }
        
        @Override
        public synchronized boolean addEdit(UndoableEdit e) {
            String editType = e instanceof CompoundEdit ? "CompoundEdit" : e.getClass().getSimpleName();
            System.err.println("DEBUG: HistoryPreservingUndoManager.addEdit for " + editType);
            System.err.println("DEBUG:   Current edits count before: " + edits.size());
            System.err.println("DEBUG:   Current limit: " + trackedLimit);
            
            // Call super.addEdit which will add the edit
            boolean result = super.addEdit(e);
            
            System.err.println("DEBUG:   Current edits count after: " + edits.size());
            System.err.println("DEBUG:   canUndo after addEdit: " + this.canUndo());
            
            return result;
        }
    }

    private static class WrapEditorKit extends StyledEditorKit {
        ViewFactory defaultFactory = new WrapColumnFactory();
        public ViewFactory getViewFactory() {
            return defaultFactory;
        }
    }



    // --- NO WRAP EDITOR KIT with LRECL character-position wrapping ---
    private static class NoWrapEditorKit extends StyledEditorKit {
        private final ViewFactory defaultFactory = new NoWrapColumnFactory();
        @Override
        public ViewFactory getViewFactory() {
            return defaultFactory;
        }
        private static class NoWrapColumnFactory implements ViewFactory {
            public View create(Element elem) {
                String kind = elem.getName();
                if (kind != null && kind.equals(AbstractDocument.ContentElementName))
                    return new LRECLGlyphView(elem);
                if (kind != null && kind.equals(AbstractDocument.ParagraphElementName))
                    return new LRECLNoWrapParagraphView(elem);
                if (kind != null && kind.equals(AbstractDocument.SectionElementName))
                    return new BoxView(elem, View.Y_AXIS);
                if (kind != null && kind.equals(StyleConstants.ComponentElementName))
                    return new ComponentView(elem);
                if (kind != null && kind.equals(StyleConstants.IconElementName))
                    return new IconView(elem);
                return new LabelView(elem);
            }
        }
        


        // GlyphView that enforces hard breaks at LRECL character positions
        private static class LRECLGlyphView extends GlyphView {
            public LRECLGlyphView(Element elem) {
                super(elem);
            }
            
            private int getMaxLineLength() {
                Container parent = getContainer();
                if (parent instanceof EditorPane) {
                    return ((EditorPane)parent).getMaxLineLength();
                }
                return 0; // 0 means no LRECL limit
            }
            



            @Override
            public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a, 
                                                 int direction, Position.Bias[] biasRet) 
                                                 throws BadLocationException {
                // Override to ensure we can navigate to the end position of this fragment
                int endOffset = getEndOffset();
                
                // Allow moving to the end offset even if it's at the fragment boundary
                if (direction == EAST && pos < endOffset) {
                    // Moving right - allow reaching the end offset
                    int next = pos + 1;
                    if (next <= endOffset) {
                        if (biasRet != null && biasRet.length > 0) {
                            biasRet[0] = Position.Bias.Forward;
                        }
                        return next;
                    }
                }
                
                return super.getNextVisualPositionFrom(pos, b, a, direction, biasRet);
            }
            

            private int getLogicalLineStartOffset() {
                // Get the document line (Element) that this view belongs to
                Element elem = getElement();
                if (elem == null) return 0;
                
                // The logical line starts at the element's start offset
                return elem.getStartOffset();
            }
            


            @Override
            public int getBreakWeight(int axis, float pos, float len) {
                if (axis == X_AXIS) {
                    int maxLen = getMaxLineLength();
                    if (maxLen <= 0) {
                        // No LRECL limit - never break
                        return BadBreakWeight;
                    }
                    
                    // Calculate position relative to the logical line start (document line)
                    int logicalLineStart = getLogicalLineStartOffset();
                    int p0 = getStartOffset();
                    int p1 = getEndOffset();
                    
                    // Don't count trailing newline in column calculations
                    try {
                        String text = getDocument().getText(p0, p1 - p0);
                        if (text.endsWith("\n") || text.endsWith("\r")) {
                            p1--;
                            if (p1 > p0 && text.length() > 1 && text.charAt(text.length() - 2) == '\r' && text.endsWith("\n")) {
                                p1--; // Handle \r\n
                            }
                        }
                    } catch (BadLocationException e) {
                        // Ignore
                    }
                    

                    // Calculate column positions relative to the logical line
                    int viewStartCol = p0 - logicalLineStart;
                    int viewEndCol = p1 - logicalLineStart;
                    
                    // Calculate which LRECL row this fragment starts and ends in (0, 1, 2, ...)
                    // Offsets are positions BETWEEN characters, so:
                    // - Position 0-16 contains characters 0-15 (row 0)
                    // - Position 16-32 contains characters 16-31 (row 1)
                    int rowStart = viewStartCol / maxLen;
                    int rowEnd = (viewEndCol > 0) ? (viewEndCol - 1) / maxLen : 0;
                    
                    if (rowEnd > rowStart) {
                        // This view spans multiple LRECL rows - must break
                        return ForcedBreakWeight;
                    }
                }
                return BadBreakWeight; // Never break for word wrapping reasons
            }
            
            @Override
            public View breakView(int axis, int p0, float pos, float len) {
                if (axis == X_AXIS) {
                    int maxLen = getMaxLineLength();
                    if (maxLen > 0) {
                        // Calculate column position relative to the logical line start
                        int logicalLineStart = getLogicalLineStartOffset();
                        int currentCol = p0 - logicalLineStart;
                        
                        // Find the next LRECL boundary
                        int currentRow = currentCol / maxLen;
                        int nextRowStart = (currentRow + 1) * maxLen;
                        
                        int p1 = getEndOffset();
                        
                        // Don't count trailing newline when breaking
                        try {
                            String text = getDocument().getText(p0, p1 - p0);
                            if (text.endsWith("\n") || text.endsWith("\r")) {
                                p1--;
                                if (p1 > p0 && text.length() > 1 && text.charAt(text.length() - 2) == '\r' && text.endsWith("\n")) {
                                    p1--; // Handle \r\n
                                }
                            }
                        } catch (BadLocationException e) {
                            // Ignore
                        }
                        
                        int endCol = p1 - logicalLineStart;
                        
                        // If this fragment extends into the next row, break at the row boundary
                        if (endCol > nextRowStart) {
                            int breakAt = logicalLineStart + nextRowStart;
                            if (breakAt > p0 && breakAt <= p1) {
                                GlyphView v = (GlyphView) createFragment(p0, breakAt);
                                return v;
                            }
                        }
                    }
                }
                return super.breakView(axis, p0, pos, len);
            }
        }
        


        // ParagraphView that respects LRECL breaking and left-aligns content
        private static class LRECLNoWrapParagraphView extends ParagraphView {
            public LRECLNoWrapParagraphView(Element elem) {
                super(elem);
            }
            
            private int getMaxLineLength() {
                Container parent = getContainer();
                if (parent instanceof EditorPane) {
                    return ((EditorPane)parent).getMaxLineLength();
                }
                return 0;
            }
            
            @Override
            protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
                SizeRequirements req = super.calculateMinorAxisRequirements(axis, r);
                int maxLen = getMaxLineLength();
                if (maxLen > 0 && axis == X_AXIS) {
                    // Limit the width requirement to LRECL
                    Container c = getContainer();
                    if (c != null) {
                        FontMetrics fm = c.getFontMetrics(c.getFont());
                        int maxWidth = fm.charWidth('W') * maxLen;
                        req.minimum = Math.min(req.minimum, maxWidth);
                        req.preferred = Math.min(req.preferred, maxWidth);
                        req.maximum = Math.min(req.maximum, maxWidth);
                    }
                }
                // Force left alignment (no centering) - alignment must be 0.0 for left-align
                req.alignment = 0.0f;
                return req;
            }
            
            @Override
            public float getAlignment(int axis) {
                // Force left alignment on X axis
                if (axis == X_AXIS) {
                    return 0.0f;
                }
                return super.getAlignment(axis);
            }
            
            @Override
            protected void layout(int width, int height) {
                super.layout(width, height);
            }
        }
    }

    private static class WrapColumnFactory implements ViewFactory {
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                switch (kind) {
                    case AbstractDocument.ContentElementName:
                        return new WrapLabelView(elem);
                    case AbstractDocument.ParagraphElementName:
                        return new LRECLParagraphView(elem);
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
    }

    private static class WrapLabelView extends LabelView {
        private int getDynamicMaxLineLength() {
            Container parent = getContainer();
            if (parent instanceof EditorPane) {
                return ((EditorPane)parent).getMaxLineLength();
            }
            return 1024;
        }
        public WrapLabelView(Element elem) {
            super(elem);
        }

        public float getMinimumSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    return 0;
                case View.Y_AXIS:
                    return super.getMinimumSpan(axis);
                default:
                    throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }

        @Override
        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            return super.modelToView(pos, a, b);
        }

        @Override
        public int getBreakWeight(int axis, float pos, float len) {
            return super.getBreakWeight(axis, pos, len);
        }

        @Override
        public View breakView(int axis, int offset, float pos, float len) {
            return super.breakView(axis, offset, pos, len);
        }

        public float getPreferredSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    float width = super.getPreferredSpan(axis);
                    int lrecl = getDynamicMaxLineLength();
                    if (lrecl > 0) {
                        FontMetrics fm = ((JTextPane)getContainer()).getFontMetrics(getFont());
                        width = Math.min(width, fm.charWidth('W') * lrecl);
                    }
                    return width;
                case View.Y_AXIS:
                    return super.getPreferredSpan(axis);
                default:
                    throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }
    }

    private int maxLineLength = 1024; // Default line length before wrap
    private boolean lineWrap = false;
    private boolean wrapStyleWord = false;
    private StyledDocument styledDoc;
    private UndoManager undoManager;
    private ProxyUndoListener proxyUndoListener;

    private boolean overtypeMode = false;
    private boolean showSpecialCharacters = false;

    /**
     * Proxy listener that can toggle between sending edits to the UndoManager
     * or suppressing edits during replace operations.
     */
    private class ProxyUndoListener implements javax.swing.event.UndoableEditListener {
        private boolean suppressEdits = false;

        @Override
        public void undoableEditHappened(javax.swing.event.UndoableEditEvent e) {
            // Skip processing if we're in suppress mode
            if (suppressEdits) {
                System.err.println("DEBUG: ProxyUndoListener - suppressing edit in replace mode");
                return;
            }
            
            if (e.getEdit().isSignificant()) {
                undoManager.addEdit(e.getEdit());
                System.err.println("DEBUG: Added to UndoManager - " + e.getEdit().getPresentationName());
            }
        }

        public void setSuppressMode(boolean suppress) {
            this.suppressEdits = suppress;
            if (suppress) {
                System.err.println("DEBUG: ProxyUndoListener - SUPPRESS mode ENABLED");
            } else {
                System.err.println("DEBUG: ProxyUndoListener - SUPPRESS mode DISABLED");
            }
        }

        public void startCompoundCapture(CompoundEdit compound) {
            // Deprecated - kept for compatibility
            this.suppressEdits = true;
            System.err.println("DEBUG: ProxyUndoListener.startCompoundCapture - compound mode ENABLED");
        }

        public void endCompoundCapture() {
            // Deprecated - kept for compatibility
            this.suppressEdits = false;
            System.err.println("DEBUG: ProxyUndoListener.endCompoundCapture - compound mode DISABLED");
        }
    }

    public EditorPane() {
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        styledDoc = (StyledDocument) getDocument();
        undoManager = new HistoryPreservingUndoManager();
        undoManager.setLimit(-1); // Unlimited undo
        System.err.println("DEBUG: EditorPane init - Document: " + System.identityHashCode(styledDoc) + ", UndoManager: " + System.identityHashCode(undoManager));
        attachUndoListener();
        InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "Undo");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "Redo");

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "ToggleOvertype");
        actionMap.put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canUndo()) {
                        undoManager.undo();
                    }
                } catch (Exception ex) {}
            }
        });
        actionMap.put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canRedo()) {
                        undoManager.redo();
                    }
                } catch (Exception ex) {}
            }
        });

        actionMap.put("ToggleOvertype", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleOvertypeMode();
            }
        });
        putClientProperty("FileChooser.enableLocking", Boolean.FALSE);
        setLineWrap(false);
    }

    /**
     * Attach the undo manager as a listener to the current document.
     * This must be called whenever the document changes (e.g., after setEditorKit).
     */
    private void attachUndoListener() {
        StyledDocument newDoc = (StyledDocument) getDocument();
        if (newDoc == null) {
            return;
        }
        
        // If the document has changed, we need to update undo manager attachment
        if (styledDoc != newDoc) {
            // Remove old undo manager from old document
            if (styledDoc != null) {
                styledDoc.removeUndoableEditListener(undoManager);
                System.err.println("DEBUG: Removed UndoManager from old document " + System.identityHashCode(styledDoc));
            }
            
            // Attach undo manager directly to new document
            newDoc.addUndoableEditListener(undoManager);
            System.err.println("DEBUG: Attached UndoManager directly to document " + System.identityHashCode(newDoc));
        } else if (styledDoc == null) {
            // First time - attach the undo manager
            newDoc.addUndoableEditListener(undoManager);
            System.err.println("DEBUG: Attached UndoManager directly to document " + System.identityHashCode(newDoc));
        }
        
        styledDoc = newDoc;
    }


    public void setMaxLineLength(int length) {
        if (this.maxLineLength != length) {
            this.maxLineLength = length;
            // Force complete view rebuild by resetting the EditorKit
            // This is necessary because the view hierarchy caches layout information
            if (!lineWrap) {
                // Save current state
                String content = getText();
                int caretPos = getCaretPosition();
                
                // Reset the EditorKit to force view rebuild
                setEditorKit(new NoWrapEditorKit());
                setText(content);
                
                // Restore caret position
                try {
                    setCaretPosition(Math.min(caretPos, content.length()));
                } catch (IllegalArgumentException e) {
                    setCaretPosition(0);
                }
                
                // Re-attach the undo manager to the new document
                attachUndoListener();
            }
        }
    }

    public int getMaxLineLength() {
        return maxLineLength;
    }


    public void setLineWrap(boolean wrap) {
        this.lineWrap = wrap;
        System.err.println("DEBUG: setLineWrap called, current Document: " + System.identityHashCode(getDocument()));
        String content = getText();
        if (wrap) {
            setEditorKit(new WrapEditorKit());
        } else {
            setEditorKit(new NoWrapEditorKit());
        }
        System.err.println("DEBUG: After setEditorKit, new Document: " + System.identityHashCode(getDocument()));
        setText(content);
        // Re-attach the undo manager to the new document
        attachUndoListener();
        revalidate();
        repaint();
    }

    public boolean getLineWrap() {
        return lineWrap;
    }

    public void setWrapStyleWord(boolean word) {
        this.wrapStyleWord = word;
    }

    public boolean getWrapStyleWord() {
        return wrapStyleWord;
    }

    public void setShowSpecialCharacters(boolean show) {
        if (this.showSpecialCharacters != show) {
            this.showSpecialCharacters = show;
            // Save current state
            String content = getText();
            int caretPos = getCaretPosition();
            
            if (show) {
                // Switch to SpecialCharactersEditorKit
                SpecialCharactersEditorKit kit = new SpecialCharactersEditorKit();
                kit.setShowSpecialCharacters(true);
                setEditorKit(kit);
            } else {
                // Switch back to normal editor kit
                if (lineWrap) {
                    setEditorKit(new WrapEditorKit());
                } else {
                    setEditorKit(new NoWrapEditorKit());
                }
            }
            
            // Restore content and caret position
            setText(content);
            try {
                setCaretPosition(Math.min(caretPos, content.length()));
            } catch (IllegalArgumentException e) {
                setCaretPosition(0);
            }
            
            // Re-attach the undo manager to the new document
            attachUndoListener();
            revalidate();
            repaint();
        }
    }

    public boolean isShowSpecialCharacters() {
        return showSpecialCharacters;
    }

    public String getCursorPosition() {
        try {
            int caretPos = getCaretPosition();
            StyledDocument doc = (StyledDocument)getDocument();
            Element root = doc.getDefaultRootElement();
            int lineNum = root.getElementIndex(caretPos) + 1;
            Element lineElem = root.getElement(lineNum - 1);
            int colNum = caretPos - lineElem.getStartOffset() + 1;
            return String.format("Ln %d, Col %d", lineNum, colNum);
        } catch (Exception e) {
            return "Ln 1, Col 1";
        }
    }

    public int getLineHeight() {
        FontMetrics fm = getFontMetrics(getFont());
        return fm.getHeight();
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public void clearUndoHistory() {
        System.err.println("DEBUG: clearUndoHistory called, Document: " + System.identityHashCode(getDocument()) + ", styledDoc: " + System.identityHashCode(styledDoc));
        System.err.println("DEBUG: UndoManager canUndo before clear: " + undoManager.canUndo() + ", canRedo: " + undoManager.canRedo());
        undoManager.discardAllEdits();
        System.err.println("DEBUG: UndoManager canUndo after clear: " + undoManager.canUndo());
    }


    /**
     * Temporarily disable undo tracking. Call enableUndoTracking() to re-enable.
     * Used during large file loads to prevent excessive undo entry creation.
     */
    public void disableUndoTracking() {
        StyledDocument doc = (StyledDocument) getDocument();
        if (undoManager != null && doc != null) {
            doc.removeUndoableEditListener(undoManager);
            System.err.println("DEBUG: UndoManager disabled for document " + System.identityHashCode(doc));
        }
    }

    /**
     * Re-enable undo tracking after it was disabled by disableUndoTracking().
     */
    public void enableUndoTracking() {
        StyledDocument doc = (StyledDocument) getDocument();
        if (undoManager != null && doc != null) {
            doc.addUndoableEditListener(undoManager);
            System.err.println("DEBUG: UndoManager re-enabled for document " + System.identityHashCode(doc));
        }
    }

    /**
     * Start capturing edits into a CompoundEdit instead of the UndoManager.
     * Used by Replace All to group multiple edits into a single undo action.
     */
    public void startCompoundCapture(CompoundEdit compound) {
        // Ensure the proxy listener is attached to the current document
        attachUndoListener();
        
        if (proxyUndoListener != null) {
            proxyUndoListener.startCompoundCapture(compound);
            System.err.println("DEBUG: Started compound edit capture on proxy for document " + System.identityHashCode(getDocument()));
        }
    }

    /**
     * Stop capturing edits into a CompoundEdit and resume normal UndoManager operations.
     */
    public void endCompoundCapture() {
        if (proxyUndoListener != null) {
            proxyUndoListener.endCompoundCapture();
            System.err.println("DEBUG: Ended compound edit capture on proxy for document " + System.identityHashCode(getDocument()));
        }
    }

    /**
     * Get the ProxyUndoListener for manual listener management.
     */
    public javax.swing.event.UndoableEditListener getProxyUndoListener() {
        return proxyUndoListener;
    }

    public void toggleOvertypeMode() {
        overtypeMode = !overtypeMode;
        updateCursorStyle();
        firePropertyChange("overtypeMode", !overtypeMode, overtypeMode);
    }

    public boolean isOvertypeMode() {
        return overtypeMode;
    }

    public void setOvertypeMode(boolean overtype) {
        boolean oldValue = this.overtypeMode;
        this.overtypeMode = overtype;
        updateCursorStyle();
        firePropertyChange("overtypeMode", oldValue, overtype);
    }


    private void updateCursorStyle() {
        // Save current caret position before changing caret
        int currentPosition = getCaretPosition();
        Caret oldCaret = getCaret();
        
        if (oldCaret != null) {
            if (overtypeMode) {
                // Create a custom caret that appears as a box
                DefaultCaret newCaret = new DefaultCaret() {
                    @Override
                    protected synchronized void damage(Rectangle r) {
                        if (r != null) {
                            // Make the cursor width match character width
                            try {
                                FontMetrics fm = getFontMetrics(getFont());
                                r.width = fm.charWidth('W'); // Use width of a typical character
                            } catch (Exception e) {
                                r.width = 8; // Fallback width
                            }
                            x = r.x;
                            y = r.y;
                            width = r.width;
                            height = r.height;
                            repaint();
                        }
                    }
                    
                    @Override
                    public void paint(Graphics g) {
                        if (isVisible()) {
                            try {
                                JTextComponent component = getComponent();
                                Rectangle r = component.modelToView2D(getDot()).getBounds();
                                g.setColor(component.getCaretColor());
                                FontMetrics fm = g.getFontMetrics();
                                int charWidth = fm.charWidth('W');
                                // Draw filled rectangle for box cursor
                                g.fillRect(r.x, r.y, charWidth, r.height);
                                // Draw the character in inverse video
                                try {
                                    int pos = getDot();
                                    if (pos < component.getDocument().getLength()) {
                                        String ch = component.getDocument().getText(pos, 1);
                                        g.setColor(component.getBackground());
                                        g.drawString(ch, r.x, r.y + fm.getAscent());
                                    }
                                } catch (BadLocationException e) {
                                    // Ignore
                                }
                            } catch (BadLocationException e) {
                                // Ignore
                            }
                        }
                    }
                };
                setCaret(newCaret);
                newCaret.setBlinkRate(500);
                // Restore caret position
                setCaretPosition(currentPosition);
            } else {
                // Standard line cursor for insert mode
                DefaultCaret newCaret = new DefaultCaret();
                setCaret(newCaret);
                newCaret.setBlinkRate(500);
                // Restore caret position
                setCaretPosition(currentPosition);
                // Force repaint to clear the old box cursor
                repaint();
            }
        }
    }

    @Override
    public void replaceSelection(String content) {
        if (overtypeMode && content != null && content.length() == 1 && !content.equals("\n")) {
            try {
                int caretPos = getCaretPosition();
                Document doc = getDocument();
                int docLength = doc.getLength();
                
                // Check if we're not at the end of the document and not at the end of a line
                if (caretPos < docLength) {
                    String nextChar = doc.getText(caretPos, 1);
                    // Don't overwrite newline characters
                    if (!nextChar.equals("\n")) {
                        // Remove the next character before inserting
                        doc.remove(caretPos, 1);
                    }
                }
                // Insert the new character
                doc.insertString(caretPos, content, null);
            } catch (BadLocationException e) {
                // Fall back to normal insert if there's an error
                super.replaceSelection(content);
            }
        } else {
            // Normal insert mode or special characters
            super.replaceSelection(content);
        }
    }
}
