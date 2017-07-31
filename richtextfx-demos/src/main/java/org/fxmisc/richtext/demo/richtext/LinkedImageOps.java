package org.fxmisc.richtext.demo.richtext;

import org.fxmisc.richtext.model.NodeSegmentOpsBase;

public class LinkedImageOps<S> extends NodeSegmentOpsBase<LinkedImage<S>, S> {

    public LinkedImageOps() {
        super(new EmptyLinkedImage<>());
    }

    @Override
    public S realGetStyle(LinkedImage<S> linkedImage) {
        return linkedImage.getStyle();
    }

    @Override
    public LinkedImage<S> realSetStyle(LinkedImage<S> linkedImage, S style) {
        return linkedImage.setStyle(style);
    }

}
