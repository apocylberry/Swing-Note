package org.foss.apocylberry.jsnote;

import javax.swing.text.*;

// This view enforces hard breaks at every LRECL chars, regardless of word wrap
public class LRECLWrappedView extends WrappedPlainView {
    private int maxLineLength;
    public LRECLWrappedView(Element elem, int maxLineLength) {
        super(elem, true); // always wrap
        this.maxLineLength = maxLineLength;
    }
    @Override
    protected int calculateBreakPosition(int p0, int p1) {
        if (maxLineLength > 0 && (p1 - p0) > maxLineLength) {
            return p0 + maxLineLength;
        }
        return super.calculateBreakPosition(p0, p1);
    }
}
