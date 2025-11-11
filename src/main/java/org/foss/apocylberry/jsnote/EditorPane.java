package org.foss.apocylberry.jsnote;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.undo.*;

public class EditorPane extends JTextPane {
    private static class WrapEditorKit extends StyledEditorKit {
        ViewFactory defaultFactory = new WrapColumnFactory();
        public ViewFactory getViewFactory() {
            return defaultFactory;
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
                        return new ParagraphView(elem);
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
        private int maxLineLength;
        
        public WrapLabelView(Element elem) {
            super(elem);
            maxLineLength = 1024; // Default
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
            if (axis == View.X_AXIS && maxLineLength > 0) {
                try {
                    // Get the text for this view
                    String text = getDocument().getText(getStartOffset(), getEndOffset() - getStartOffset());
                    
                    // Always force a break at LRECL regardless of word wrap
                    int lrecl = maxLineLength;
                    if (lrecl > 0 && text.length() > lrecl) {
                        return BadBreakWeight;
                    }
                } catch (BadLocationException e) {
                    // Fall back to default behavior
                }
            }
            return super.getBreakWeight(axis, pos, len);
        }
        
        @Override
        public View breakView(int axis, int offset, float pos, float len) {
    if (axis == View.X_AXIS && maxLineLength > 0) {
        int lrecl = maxLineLength;
        int breakPoint = findBreakPoint(offset, lrecl);
        if (breakPoint >= 0) {
            // Recursively break at every LRECL chunk, not dependent on lineWrap
            int textLength;
            try {
                textLength = getDocument().getText(offset, getEndOffset() - offset).length();
            } catch (BadLocationException e) {
                textLength = 0;
            }
            if (textLength > lrecl) {
                return createFragment(offset, offset + lrecl);
            }
        }
    }
    return super.breakView(axis, offset, pos, len);
}
        
        private int findBreakPoint(int offset, int lrecl) {
    try {
        String text = getDocument().getText(offset, getEndOffset() - offset);
        if (lrecl > 0 && text.length() > lrecl) {
            // Forcibly break at every LRECL until done
            return lrecl;
        }
    } catch (BadLocationException e) {
        // Fall back to default behavior
    }
    return -1;
}
        
        public float getPreferredSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    float width = super.getPreferredSpan(axis);
                    if (maxLineLength > 0) {
                        FontMetrics fm = ((JTextPane)getContainer()).getFontMetrics(getFont());
                        width = Math.min(width, fm.charWidth('W') * maxLineLength);
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
    
    public EditorPane() {
        // Set default font to monospace
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        // Set up the styled document with custom editor kit
        EditorKit editorKit = new WrapEditorKit();
        setEditorKit(editorKit);
        styledDoc = (StyledDocument) getDocument();
        
        // Set up unlimited undo/redo support
        undoManager = new UndoManager();
        undoManager.setLimit(-1); // Unlimited undo
        // Add undo support to the document
        styledDoc.addUndoableEditListener(e -> {
            if (e.getEdit().isSignificant()) {
                undoManager.addEdit(e.getEdit());
            }
        });
        
        // Add Undo/Redo key bindings
        InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "Undo");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "Redo");
        
        actionMap.put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canUndo()) {
                        undoManager.undo();
                    }
                } catch (Exception ex) {
                    // Ignore undo errors
                }
            }
        });
        
        actionMap.put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canRedo()) {
                        undoManager.redo();
                    }
                } catch (Exception ex) {
                    // Ignore redo errors
                }
            }
        });
        
        // Set up custom editor kit for special characters and wrapping
        setEditorKit(new SpecialCharactersEditorKit());
        
        // Disable file locking behavior
        putClientProperty("FileChooser.enableLocking", Boolean.FALSE);
    }
    
    public void setMaxLineLength(int length) {
        this.maxLineLength = length;
    }
    
    public int getMaxLineLength() {
        return maxLineLength;
    }
    
    public void setLineWrap(boolean wrap) {
        this.lineWrap = wrap;
        String content = getText();
        if (wrap) {
            setEditorKit(new WrapEditorKit());
        } else {
            setEditorKit(new SpecialCharactersEditorKit());
        }
        setText(content);
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
    
    // Get cursor position: both line and column 1-based, column counts chars after last newline (not including newline)
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
}
