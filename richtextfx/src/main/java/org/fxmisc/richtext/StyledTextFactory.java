package org.fxmisc.richtext;

import java.util.function.BiConsumer;

import org.fxmisc.richtext.model.Segment;

import javafx.geometry.VPos;
import javafx.scene.Node;

public class StyledTextFactory<S> implements SegmentFactory<S> {

    @Override
    public Node createNode(Segment<S> segment, BiConsumer<? super TextExt, S> applyStyle) {
      TextExt t = new TextExt(segment.getText());
      t.setTextOrigin(VPos.TOP);
      t.getStyleClass().add("text");
      applyStyle.accept(t, segment.getStyle());

      // XXX: binding selectionFill to textFill,
      // see the note at highlightTextFill
      t.impl_selectionFillProperty().bind(t.fillProperty());

      return t;
    }
}
