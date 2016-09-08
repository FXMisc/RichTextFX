package org.fxmisc.richtext.model;


/**
 * This is the base class for custom objects in the model layer.
 * Its String representation is always one character long and contains
 * the "object replacement character" (\ufffc). 
 *
 * @param <S>
 */
public abstract class CustomObject<S> implements Segment<S> {

    private S style;
    private SegmentType typeId;

    public CustomObject(S style, SegmentType typeId) {
        this.style = style;
        this.typeId = typeId;
    }


    @Override
    public Segment<S> subSequence(int start, int end) {
        if (start == 0 && end == 1) {
            return this;
        }
        return new StyledText<>("", getStyle());
    }


    @Override
    public Segment<S> subSequence(int start) {
        if (start == 1) {
            return new StyledText<>("", getStyle());
        }
        return this;
    }


    @Override
    public Segment<S> append(String str) {
        throw new UnsupportedOperationException();
        // return new StyledText<>(text + str, style);
    }


    @Override
    public Segment<S> spliced(int from, int to, CharSequence replacement) {
        throw new UnsupportedOperationException();
/*        String left = text.substring(0, from);
        String right = text.substring(to);
        return new StyledText<>(left + replacement + right, style);*/
    }


    @Override
    public int length() {
        return 1;
    }


    @Override
    public char charAt(int index) {
        return getText().charAt(0);
    }


    @Override
    public String getText() {
        return "\ufffc";
    }


    @Override
    public S getStyle() {
        return style;
    }


    @Override
    public SegmentType getTypeId() {
        return typeId;
    }
}
