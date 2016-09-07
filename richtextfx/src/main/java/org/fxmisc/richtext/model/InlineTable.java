package org.fxmisc.richtext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class InlineTable<S> extends CustomObject<S> {


    public InlineTable(S style) {
        super(style, DefaultSegmentTypes.INLINE_TABLE);
    }

    @Override
    public String toString() {
        return String.format("InlineTable[objectData=%s]", null);
    }

    @Override
    public void encode(DataOutputStream os) {
    }


    public static <S> Segment<S> decode(DataInputStream is, Codec<S> styleCodec) throws IOException {
        return null;
    }
}
