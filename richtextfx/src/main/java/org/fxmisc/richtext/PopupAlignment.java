package org.fxmisc.richtext;

import static org.fxmisc.richtext.PopupAlignment.HorizontalAlignment.*;
import static org.fxmisc.richtext.PopupAlignment.AnchorObject.*;
import static org.fxmisc.richtext.PopupAlignment.VerticalAlignment.*;

public enum PopupAlignment {
    CARET_TOP(CARET, TOP, H_CENTER),
    CARET_CENTER(CARET, V_CENTER, H_CENTER),
    CARET_BOTTOM(CARET, BOTTOM, H_CENTER),
    SELECTION_TOP_LEFT(SELECTION, TOP, LEFT),
    SELECTION_TOP_CENTER(SELECTION, TOP, H_CENTER),
    SELECTION_TOP_RIGHT(SELECTION, TOP, RIGHT),
    SELECTION_CENTER_LEFT(SELECTION, V_CENTER, LEFT),
    SELECTION_CENTER(SELECTION, V_CENTER, H_CENTER),
    SELECTION_CENTER_RIGHT(SELECTION, V_CENTER, RIGHT),
    SELECTION_BOTTOM_LEFT(SELECTION, BOTTOM, LEFT),
    SELECTION_BOTTOM_CENTER(SELECTION, BOTTOM, H_CENTER),
    SELECTION_BOTTOM_RIGHT(SELECTION, BOTTOM, RIGHT);

    public static enum AnchorObject {
        CARET,
        SELECTION,
    }
    
    public static enum VerticalAlignment {
        TOP,
        V_CENTER,
        BOTTOM,
    }
    
    public static enum HorizontalAlignment {
        LEFT,
        H_CENTER,
        RIGHT,
    }

    private AnchorObject anchor;
    private VerticalAlignment vAlign;
    private HorizontalAlignment hAlign;

    private PopupAlignment(
            AnchorObject anchor,
            VerticalAlignment vAlign,
            HorizontalAlignment hAlign) {
        this.anchor = anchor;
        this.vAlign = vAlign;
        this.hAlign = hAlign;
    }

    public AnchorObject getAnchorObject() { return anchor; }
    public VerticalAlignment getVerticalAlignment() { return vAlign; }
    public HorizontalAlignment getHorizontalAlignment() { return hAlign; }
}
