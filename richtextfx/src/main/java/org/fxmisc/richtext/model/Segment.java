package org.fxmisc.richtext.model;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * An interface to segment types, like StyledText or InlineImage.
 *
 * @param <S>
 */
public interface Segment<S> {

    int length();

    char charAt(int index);

    String getText();   // each segment has a string associated with it - for custom objects
                        // this is the replacement character \ufffc 

    Segment<S> subSequence(int start, int end);

    Segment<S> subSequence(int start);

    Segment<S> append(String str);

    Segment<S> spliced(int from, int to, CharSequence replacement);

    S getStyle();

    void setStyle(S style);

    SegmentType getTypeId();        // ????????????????

    void encode(DataOutputStream os) throws IOException;
}
