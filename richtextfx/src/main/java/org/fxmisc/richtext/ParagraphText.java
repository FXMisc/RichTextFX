package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

import org.reactfx.value.Val;
import org.reactfx.value.Var;

class ParagraphText<PS, S> extends TextFlowExt {

    // FIXME: changing it currently has not effect, because
    // Text.impl_selectionFillProperty().set(newFill) doesn't work
    // properly for Text node inside a TextFlow (as of JDK8-b100).
    private final ObjectProperty<Paint> highlightTextFill = new SimpleObjectProperty<>(Color.WHITE);
    public ObjectProperty<Paint> highlightTextFillProperty() {
        return highlightTextFill;
    }

    private final Var<Integer> caretPosition = Var.newSimpleVar(0);
    public Var<Integer> caretPositionProperty() { return caretPosition; }
    public void setCaretPosition(int pos) { caretPosition.setValue(pos); }
    private final Val<Integer> clampedCaretPosition;

    private final ObjectProperty<IndexRange> selection = new SimpleObjectProperty<>(StyledTextArea.EMPTY_RANGE);
    public ObjectProperty<IndexRange> selectionProperty() { return selection; }
    public void setSelection(IndexRange sel) { selection.set(sel); }

    private final Paragraph<PS, S> paragraph;

    private final Path caretShape = new Path();
    private final Path selectionShape = new Path();
    private final List<Path> backgroundShapes = new ArrayList<>();

    // proxy for caretShape.visibleProperty() that implements unbind() correctly.
    // This is necessary due to a bug in BooleanPropertyBase#unbind().
    // See https://bugs.openjdk.java.net/browse/JDK-8130458
    private final Var<Boolean> caretVisible = Var.newSimpleVar(false);
    {
        caretShape.visibleProperty().bind(caretVisible);
    }

    public ParagraphText(Paragraph<PS, S> par, BiConsumer<? super TextExt, S> applyStyle) {
        this.paragraph = par;

        getStyleClass().add("paragraph-text");

        int parLen = paragraph.length();
        clampedCaretPosition = caretPosition.map(i -> Math.min(i, parLen));
        clampedCaretPosition.addListener((obs, oldPos, newPos) -> requestLayout());

        selection.addListener((obs, old, sel) -> requestLayout());

        Val<Double> leftInset = Val.map(insetsProperty(), Insets::getLeft);
        Val<Double> topInset = Val.map(insetsProperty(), Insets::getTop);

        // selection highlight
        selectionShape.setManaged(false);
        selectionShape.setFill(Color.DODGERBLUE);
        selectionShape.setStrokeWidth(0);
        selectionShape.layoutXProperty().bind(leftInset);
        selectionShape.layoutYProperty().bind(topInset);
        getChildren().add(selectionShape);

        // caret
        caretShape.getStyleClass().add("caret");
        caretShape.setManaged(false);
        caretShape.setStrokeWidth(1);
        caretShape.layoutXProperty().bind(leftInset);
        caretShape.layoutYProperty().bind(topInset);
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
            TextExt t = new TextExt(segment.getText());
            t.setTextOrigin(VPos.TOP);
            t.getStyleClass().add("text");
            applyStyle.accept(t, segment.getStyle());

            // XXX: binding selectionFill to textFill,
            // see the note at highlightTextFill
            t.impl_selectionFillProperty().bind(t.fillProperty());

            getChildren().add(t);

            // add corresponding background node (empty)

            Path backgroundShape = new Path();
            backgroundShape.setManaged(false);
            backgroundShape.setStrokeWidth(0);
            backgroundShape.layoutXProperty().bind(leftInset);
            backgroundShape.layoutYProperty().bind(topInset);
            backgroundShapes.add(backgroundShape);
            getChildren().add(0, backgroundShape);
        }
    }

    public Paragraph<PS, S> getParagraph() {
        return paragraph;
    }

    public Var<Boolean> caretVisibleProperty() {
        return caretVisible;
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
        return getLineOfCharacter(clampedCaretPosition.getValue());
    }

    private void updateCaretShape() {
        PathElement[] shape = getCaretShape(clampedCaretPosition.getValue(), true);
        caretShape.getElements().setAll(shape);
    }

    private void updateSelectionShape() {
        int start = selection.get().getStart();
        int end = selection.get().getEnd();
        PathElement[] shape = getRangeShape(start, end);
        selectionShape.getElements().setAll(shape);
    }

    private void updateBackgroundShapes() {
        int index = 0;
        int start = 0;

        FilteredList<Node> nodeList = getChildren().filtered(node -> node instanceof TextExt);
        for (Node node : nodeList) {
            TextExt text = (TextExt) node;
            Path backgroundShape = backgroundShapes.get(index++);
            int end = start + text.getText().length();

            // Set fill
            Paint paint = text.backgroundFillProperty().get();
            if (paint != null) {
                backgroundShape.setFill(paint);

                // Set path elements
                PathElement[] shape = getRangeShape(start, end);
                backgroundShape.getElements().setAll(shape);
            }

            start = end;
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        updateCaretShape();
        updateSelectionShape();
        updateBackgroundShapes();
    }
}
