package org.fxmisc.richtext.j9adapters;

import javafx.scene.shape.PathElement;

public interface TextLayout
{
    public static final int TYPE_TEXT           = 1 << 0;
    public static final int TYPE_UNDERLINE      = 1 << 1;

    TextLine[] getLines();

    int getLineIndex(float y);

    int getCharCount();

    HitInfo getHitInfo(float x, float y);
    PathElement[] getCaretShape(int offset, boolean isLeading, float x, float y);
    PathElement[] getRange(int start, int end, int type, float x, float y);
}
