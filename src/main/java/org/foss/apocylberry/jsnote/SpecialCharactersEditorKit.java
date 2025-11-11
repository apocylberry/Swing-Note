package org.foss.apocylberry.jsnote;

import javax.swing.text.*;

public class SpecialCharactersEditorKit extends StyledEditorKit {
    @Override
    public ViewFactory getViewFactory() {
        return new ViewFactory() {
            public View create(Element elem) {
                // Use EditorPane's maxLineLength for LRECL
                int lrecl = 1024;
                JTextComponent comp = (JTextComponent) elem.getDocument().getProperty("filterNewlines");
                if (comp instanceof EditorPane) {
                    lrecl = ((EditorPane) comp).getMaxLineLength();
                }
                return new LRECLWrappedView(elem, lrecl);
            }
        };
    }

}
