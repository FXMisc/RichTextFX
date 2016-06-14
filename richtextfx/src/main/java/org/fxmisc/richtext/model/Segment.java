package org.fxmisc.richtext.model;

/**
 * An interface to segment types, like StyledText or CustomObject.
 *
 * @param <S>
 */
public interface Segment<S> {

    int length();

    char charAt(int index);

    String getText();

    Segment<S> subSequence(int start, int end);

    Segment<S> subSequence(int start);

    Segment<S> append(String str);

    Segment<S> spliced(int from, int to, CharSequence replacement);

    S getStyle();

    void setStyle(S style);

    SegmentType getTypeId();
}
