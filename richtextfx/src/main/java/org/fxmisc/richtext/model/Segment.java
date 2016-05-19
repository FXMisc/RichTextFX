package org.fxmisc.richtext.model;

/**
 * An interface to segment types, like StyledText or CustomObject.
 *
 * @param <S>
 */
public interface Segment<S> {

    public int length();

    public char charAt(int index);

    public String getText();

    public Segment<S> subSequence(int start, int end);

    public Segment<S> subSequence(int start);

    public Segment<S> append(String str);

    public Segment<S> spliced(int from, int to, CharSequence replacement);

    public S getStyle();

    public void setStyle(S style);
    
    int getTypeId();
}
