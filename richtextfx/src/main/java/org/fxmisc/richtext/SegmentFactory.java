package org.fxmisc.richtext;

import java.util.function.BiConsumer;

import org.fxmisc.richtext.model.Segment;

import javafx.scene.Node;

public interface SegmentFactory<S> {

    Node createNode(Segment<S> segment);
}
