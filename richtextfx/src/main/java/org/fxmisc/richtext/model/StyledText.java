package org.fxmisc.richtext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class StyledText<S> implements Segment<S> {
    private final String text;
    private /*final*/ S style;

    public StyledText(String text, S style) {
        this.text = text;
        this.style = style;
    }

    @Override
    public int length() {
        return text.length();
    }

    @Override
    public char charAt(int index) {
        return text.charAt(index);
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Segment<S> subSequence(int start, int end) {
        return new StyledText<>(text.substring(start, end), style);
    }

    @Override
    public Segment<S> subSequence(int start) {
        return new StyledText<>(text.substring(start), style);
    }

    @Override
    public Segment<S> append(String str) {
        return new StyledText<>(text + str, style);
    }

    @Override
    public Segment<S> spliced(int from, int to, CharSequence replacement) {
        String left = text.substring(0, from);
        String right = text.substring(to);
        return new StyledText<>(left + replacement + right, style);
    }

    @Override
    public S getStyle() {
        return style;
    }

    @Override
    public void setStyle(S style) {
        this.style = style;
    }

    @Override
    public String toString() {
        return '"' + text + '"' + ":" + style;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof StyledText) {
            StyledText<?> that = (StyledText<?>) obj;
            return Objects.equals(this.text, that.text)
                && Objects.equals(this.style, that.style);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, style);
    }

    @Override
    public SegmentType getTypeId() {
        return DefaultSegmentTypes.STYLED_TEXT;
    }

    @Override
    public void encode(DataOutputStream os) throws IOException {
        Codec.STRING_CODEC.encode(os, getText());
    }


    public static <S> Segment<S> decode(DataInputStream is, Codec<S> styleCodec) throws IOException {
        String text = Codec.STRING_CODEC.decode(is);
        S style = styleCodec.decode(is);
        return new StyledText<>(text, style);
    }
}
