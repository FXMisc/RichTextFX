package org.fxmisc.richtext;

public interface TextLayout {
    TextLine[] getLines();

    int getLineIndex(float y);

    int getCharCount();
}
