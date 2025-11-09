package org.foss.apocylberry.jsnote;

import javax.swing.text.*;

public class SpecialCharactersEditorKit extends StyledEditorKit {
    private static SpecialCharactersView view;
    
    @Override
    public ViewFactory getViewFactory() {
        return new ViewFactory() {
            public View create(Element elem) {
                view = new SpecialCharactersView(elem);
                return view;
            }
        };
    }
    
    public static void setShowSpecialCharacters(boolean show) {
        if (view != null) {
            view.setShowSpecialCharacters(show);
        }
    }
}
