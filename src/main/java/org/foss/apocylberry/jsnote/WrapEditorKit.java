package org.foss.apocylberry.jsnote;

import javax.swing.text.*;

public class WrapEditorKit extends StyledEditorKit {
    public static class WrapDocument extends PlainDocument {
        private int maxLineLength = 1024;

        public void setMaxLineLength(int length) {
            this.maxLineLength = length;
        }

        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            if (str == null || maxLineLength <= 0) {
                super.insertString(offs, str, a);
                return;
            }

            // Get the line start offset
            int lineStart = offs;
            String content = getText(0, offs);
            int lastNewline = content.lastIndexOf('\n');
            if (lastNewline != -1) {
                lineStart = lastNewline + 1;
            }

            // Calculate current line length
            int lineLength = offs - lineStart;
            
            // If inserting would exceed max length, insert newline first
            if (lineLength + str.length() > maxLineLength) {
                super.insertString(offs, "\n" + str, a);
            } else {
                super.insertString(offs, str, a);
            }
        }
    }
}
