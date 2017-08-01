package org.fxmisc.richtext.model;

public abstract class TextOpsBase<SEG, S> extends SegmentOpsBase<SEG, S> implements TextOps<SEG, S> {

    TextOpsBase(SEG empty) {
        super(empty);
    }
}
