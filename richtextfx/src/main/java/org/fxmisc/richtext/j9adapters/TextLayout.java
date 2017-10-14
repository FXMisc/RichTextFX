package org.fxmisc.richtext.j9adapters;

public interface TextLayout
{
    TextLine[] getLines();

    int getLineIndex(float y);

    int getCharCount();
}
