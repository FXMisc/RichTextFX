package org.fxmisc.richtext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This is the base class for custom objects in the model layer.
 * Its String representation is always one character long and contains
 * the "object replacement character" (\ufffc). 
 */
public abstract class CustomObject<S> implements Segment<S> {

    protected S style;

    protected CustomObject() {}

    public CustomObject(S style) {
        this.style = style;
    }

    
    @Override
    public Segment<S> subSequence(int start, int end) {
        if (start == 0 && end == 1) {
            return this;
        }
        return new StyledText<>("", style);
    }


    @Override
    public Segment<S> subSequence(int start) {
        if (start == 1) {
            return new StyledText<>("", style);
        }
        return this;
    }

    
    @Override
    public CustomObject<S> append(String str) {
        throw new UnsupportedOperationException();
        // return new StyledText<>(text + str, style);
    }


    @Override
    public CustomObject<S> spliced(int from, int to, CharSequence replacement) {
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
    public void setStyle(S style) {
        this.style = style;
    }

    public abstract void encode(DataOutputStream os) throws IOException;

    @Override
    public final void encode(DataOutputStream os, Codec<S> styleCodec) throws IOException {
        encode(os);
        styleCodec.encode(os, style);
    }

    public abstract void decode(DataInputStream is) throws IOException;

    @Override
    public final void decode(DataInputStream is, Codec<S> styleCodec) throws IOException {
        decode(is);
        style = styleCodec.decode(is);
    }

    @Override
    public boolean canJoin(Segment<S> right) {
        return false;
    }
}
