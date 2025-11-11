package org.foss.apocylberry.jsnote;

import javax.swing.text.*;

public class SpecialCharactersEditorKit extends StyledEditorKit {
    @Override
    public ViewFactory getViewFactory() {
        return new ViewFactory() {
            public View create(Element elem) {
                return new LRECLWrappedView(elem, 1024);
            }
        };
    }

}
