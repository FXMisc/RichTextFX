package org.fxmisc.richtext.demo.richtext;

import org.fxmisc.richtext.model.NodeSegmentOpsBase;


public class LinkedImageOps<S> extends NodeSegmentOpsBase<LinkedImage, S> {

    public LinkedImageOps() {
        super(new EmptyLinkedImage());
    }

    @Override
    public int length(LinkedImage linkedImage) {
        return linkedImage.isReal() ? 1 : 0;
    }

}
