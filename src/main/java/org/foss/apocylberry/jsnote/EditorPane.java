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

    // --- NO WRAP EDITOR KIT (for wrap=false) ---
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
                    return new NoWrapLabelView(elem);
                if (kind != null && kind.equals(AbstractDocument.ParagraphElementName))
                    return new ParagraphView(elem);
                if (kind != null && kind.equals(AbstractDocument.SectionElementName))
                    return new BoxView(elem, View.Y_AXIS);
                if (kind != null && kind.equals(StyleConstants.ComponentElementName))
                    return new ComponentView(elem);
                if (kind != null && kind.equals(StyleConstants.IconElementName))
                    return new IconView(elem);
                return new LabelView(elem);
            }
        }
        private static class NoWrapLabelView extends LabelView {
            public NoWrapLabelView(Element elem) { super(elem); }
            @Override
            public float getMinimumSpan(int axis) {
                if (axis == View.X_AXIS)
                    return super.getPreferredSpan(axis); // Prevent wrapping
                return super.getMinimumSpan(axis);
            }
            @Override
            public float getPreferredSpan(int axis) {
                if (axis == View.X_AXIS)
                    return super.getPreferredSpan(axis); // Prevent wrapping
                return super.getPreferredSpan(axis);
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

    public EditorPane() {
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        styledDoc = (StyledDocument) getDocument();
        undoManager = new UndoManager();
        undoManager.setLimit(-1); // Unlimited undo
        styledDoc.addUndoableEditListener(e -> {
            if (e.getEdit().isSignificant()) {
                undoManager.addEdit(e.getEdit());
            }
        });
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
        putClientProperty("FileChooser.enableLocking", Boolean.FALSE);
        setLineWrap(false);
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
            setEditorKit(new NoWrapEditorKit());
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
