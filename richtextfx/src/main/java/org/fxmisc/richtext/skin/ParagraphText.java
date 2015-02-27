package org.fxmisc.richtext.skin;

import java.util.Optional;
import java.util.function.BiConsumer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.control.IndexRange;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;

import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.StyledText;
import org.fxmisc.richtext.StyledTextArea;
import org.reactfx.value.Val;

class ParagraphText<S> extends TextFlowExt {

    // FIXME: changing it currently has not effect, because
    // Text.impl_selectionFillProperty().set(newFill) doesn't work
    // properly for Text node inside a TextFlow (as of JDK8-b100).
    private final ObjectProperty<Paint> highlightTextFill = new SimpleObjectProperty<Paint>(Color.WHITE);
    public ObjectProperty<Paint> highlightTextFillProperty() {
        return highlightTextFill;
    }

    private final IntegerProperty caretPosition = new SimpleIntegerProperty(0);
    public IntegerProperty caretPositionProperty() { return caretPosition; }
    public void setCaretPosition(int pos) { caretPosition.set(pos); }
    private final NumberBinding clampedCaretPosition;

    private final ObjectProperty<IndexRange> selection = new SimpleObjectProperty<>(StyledTextArea.EMPTY_RANGE);
    public ObjectProperty<IndexRange> selectionProperty() { return selection; }
    public void setSelection(IndexRange sel) { selection.set(sel); }

    private final Paragraph<S> paragraph;

    private final Path caretShape = new Path();
    private final Path selectionShape = new Path();

    public ParagraphText(Paragraph<S> par, BiConsumer<Text, S> applyStyle) {
        this.paragraph = par;

        getStyleClass().add("paragraph-text");

        clampedCaretPosition = Bindings.min(caretPosition, paragraph.length());
        clampedCaretPosition.addListener((obs, oldPos, newPos) -> requestLayout());

        selection.addListener((obs, old, sel) -> requestLayout());

        Val<Double> leftInset = Val.map(insetsProperty(), ins -> ins.getLeft());
        Val<Double> rightInset = Val.map(insetsProperty(), ins -> ins.getTop());

        // selection highlight
        selectionShape.setManaged(false);
        selectionShape.setVisible(true);
        selectionShape.setFill(Color.DODGERBLUE);
        selectionShape.setStrokeWidth(0);
        selectionShape.layoutXProperty().bind(leftInset);
        selectionShape.layoutYProperty().bind(rightInset);
        getChildren().add(selectionShape);

        // caret
        caretShape.getStyleClass().add("caret");
        caretShape.setManaged(false);
        caretShape.setStrokeWidth(1);
        caretShape.layoutXProperty().bind(leftInset);
        caretShape.layoutYProperty().bind(rightInset);
        getChildren().add(caretShape);

        // XXX: see the note at highlightTextFill
//        highlightTextFill.addListener(new ChangeListener<Paint>() {
//            @Override
//            public void changed(ObservableValue<? extends Paint> observable,
//                    Paint oldFill, Paint newFill) {
//                for(PumpedUpText text: textNodes())
//                    text.impl_selectionFillProperty().set(newFill);
//            }
//        });

        // populate with text nodes
        for(StyledText<S> segment: par.getSegments()) {
            Text t = new Text(segment.toString());
            t.setTextOrigin(VPos.TOP);
            t.getStyleClass().add("text");
            applyStyle.accept(t, segment.getStyle());

            // XXX: binding selectionFill to textFill,
            // see the note at highlightTextFill
            t.impl_selectionFillProperty().bind(t.fillProperty());

            getChildren().add(t);
        }
    }

    public Paragraph<S> getParagraph() {
        return paragraph;
    }

    public BooleanProperty caretVisibleProperty() {
        return caretShape.visibleProperty();
    }

    public ObjectProperty<Paint> highlightFillProperty() {
        return selectionShape.fillProperty();
    }

    public double getCaretOffsetX() {
        layout(); // ensure layout, is a no-op if not dirty
        Bounds bounds = caretShape.getLayoutBounds();
        return (bounds.getMinX() + bounds.getMaxX()) / 2;
    }

    public Bounds getCaretBounds() {
        layout(); // ensure layout, is a no-op if not dirty
        return caretShape.getBoundsInParent();
    }

    public Bounds getCaretBoundsOnScreen() {
        layout(); // ensure layout, is a no-op if not dirty
        Bounds localBounds = caretShape.getBoundsInLocal();
        return caretShape.localToScreen(localBounds);
    }

    public Optional<Bounds> getSelectionBoundsOnScreen() {
        if(selection.get().getLength() == 0) {
            return Optional.empty();
        } else {
            layout(); // ensure layout, is a no-op if not dirty
            Bounds localBounds = selectionShape.getBoundsInLocal();
            return Optional.of(selectionShape.localToScreen(localBounds));
        }
    }

    public int currentLineIndex() {
        return getLineOfCharacter(clampedCaretPosition.intValue());
    }

    private void updateCaretShape() {
        PathElement[] shape = getCaretShape(clampedCaretPosition.intValue(), true);
        caretShape.getElements().setAll(shape);
    }

    private void updateSelectionShape() {
        int start = selection.get().getStart();
        int end = selection.get().getEnd();
        PathElement[] shape = getRangeShape(start, end);
        selectionShape.getElements().setAll(shape);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        updateCaretShape();
        updateSelectionShape();
    }
}
