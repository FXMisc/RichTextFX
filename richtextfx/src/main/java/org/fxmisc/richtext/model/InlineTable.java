package org.fxmisc.richtext.model;

import java.io.DataOutputStream;

public class InlineTable<S> extends CustomObject<S> {

    
    public InlineTable(S style, SegmentType typeId) {
        super(style, typeId);
    }

    @Override
    public String toString() {
        return String.format("InlineTable[objectData=%s]", null);
    }

    @Override
    public void encode(DataOutputStream os) {
    }

}
