package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.StrokeLineCap;

import org.fxmisc.richtext.model.Paragraph;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuples;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

class ParagraphText<PS, SEG, S> extends TextFlowExt {

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

    private final Paragraph<PS, SEG, S> paragraph;

    private final Path caretShape = new Path();
    private final Path selectionShape = new Path();
    private final List<Path> backgroundShapes = new LinkedList<>();
    private final List<Path> underlineShapes;

    private final List<Tuple2<Paint, IndexRange>> backgroundColorRanges = new LinkedList<>();
    private final Val<Double> leftInset;
    private final Val<Double> topInset;

    // proxy for caretShape.visibleProperty() that implements unbind() correctly.
    // This is necessary due to a bug in BooleanPropertyBase#unbind().
    // See https://bugs.openjdk.java.net/browse/JDK-8130458
    private final Var<Boolean> caretVisible = Var.newSimpleVar(false);
    {
        caretShape.visibleProperty().bind(caretVisible);
    }

    ParagraphText(Paragraph<PS, SEG, S> par, Function<SEG, Node> nodeFactory) {
        this.paragraph = par;

        getStyleClass().add("paragraph-text");

        int parLen = paragraph.length();
        clampedCaretPosition = caretPosition.map(i -> Math.min(i, parLen));
        clampedCaretPosition.addListener((obs, oldPos, newPos) -> requestLayout());

        selection.addListener((obs, old, sel) -> requestLayout());

        leftInset = Val.map(insetsProperty(), Insets::getLeft);
        topInset = Val.map(insetsProperty(), Insets::getTop);

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
        underlineShapes = new ArrayList<>();

        // populate with text nodes
        for(SEG segment: par.getSegments()) {
            // create Segment
            Node fxNode = nodeFactory.apply(segment);
            getChildren().add(fxNode);

            // add placeholder to prevent IOOBE; only create shapes when needed
            underlineShapes.add(null);

        }
    }

    public Paragraph<PS, SEG, S> getParagraph() {
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

    public Bounds getRangeBoundsOnScreen(int from, int to) {
        layout(); // ensure layout, is a no-op if not dirty
        PathElement[] rangeShape = getRangeShape(from, to);

        // switch out shapes to calculate the bounds on screen
        // Must take a copy of the list contents, not just a reference:
        List<PathElement> selShape = new ArrayList<>(selectionShape.getElements());
        selectionShape.getElements().setAll(rangeShape);
        Bounds localBounds = selectionShape.getBoundsInLocal();
        Bounds rangeBoundsOnScreen = selectionShape.localToScreen(localBounds);
        selectionShape.getElements().setAll(selShape);

        return rangeBoundsOnScreen;
    }

    public Optional<Bounds> getSelectionBoundsOnScreen() {
        if(selection.get().getLength() == 0) {
            return Optional.empty();
        } else {
            layout(); // ensure layout, is a no-op if not dirty
            Bounds localBounds = selectionShape.getBoundsInLocal();
            return Optional.ofNullable(selectionShape.localToScreen(localBounds));
        }
    }

    public int getCurrentLineStartPosition() {
        return getLineStartPosition(clampedCaretPosition.getValue());
    }

    public int getCurrentLineEndPosition() {
        return getLineEndPosition(clampedCaretPosition.getValue());
    }

    public int currentLineIndex() {
        return getLineOfCharacter(clampedCaretPosition.getValue());
    }

    public int currentLineIndex(int position) {
        return getLineOfCharacter(position);
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
            int end = start + text.getText().length();

            calculateBackgroundColorRange(text, start, end);

            updateUnderline(text, start, end, index);

            start = end;
            index++;
        }

        updateBackgroundColorShapes();
    }

    /**
     * Calculates the range of a background color that is shared between multiple consecutive {@link TextExt} nodes
     */
    private void calculateBackgroundColorRange(TextExt text, int start, int end) {
        Paint backgroundColor = text.getBackgroundColor();
        if (backgroundColor != null) {
            Runnable addNewColor = () -> backgroundColorRanges.add(Tuples.t(backgroundColor, new IndexRange(start, end)));

            if (backgroundColorRanges.isEmpty()) {
                addNewColor.run();
            } else {
                int lastIndex = backgroundColorRanges.size() - 1;
                Tuple2<Paint, IndexRange> lastColorRange = backgroundColorRanges.get(lastIndex);
                Paint lastColor = lastColorRange._1;
                if (lastColor.equals(backgroundColor)) {
                    IndexRange colorRange = lastColorRange._2;
                    backgroundColorRanges.set(lastIndex, Tuples.t(backgroundColor, new IndexRange(colorRange.getStart(), end)));
                } else {
                    addNewColor.run();
                }
            }
        }
    }

    private void updateBackgroundColorShapes() {
        // remove or add shapes, depending on what's needed
        int neededNumber = backgroundColorRanges.size();
        int availableNumber = backgroundShapes.size();

        if (neededNumber < availableNumber) {
            List<Path> unusedShapes = backgroundShapes.subList(neededNumber, availableNumber);
            getChildren().removeAll(unusedShapes);
            unusedShapes.clear();
        } else if (availableNumber < neededNumber) {
            for (int i = 0; i < neededNumber - availableNumber; i++) {
                Path backgroundShape = new Path();
                backgroundShape.setManaged(false);
                backgroundShape.setStrokeWidth(0);
                backgroundShape.layoutXProperty().bind(leftInset);
                backgroundShape.layoutYProperty().bind(topInset);

                backgroundShapes.add(backgroundShape);
                getChildren().add(0, backgroundShape);
            }
        }

        // update the shape's color and elements
        int i = 0;
        for (Tuple2<Paint, IndexRange> t : backgroundColorRanges) {
            Path backgroundShape = backgroundShapes.get(i);
            backgroundShape.setFill(t._1);
            backgroundShape.getElements().setAll(getRangeShape(t._2));
            i++;
        }

        // clear, since it's no longer needed
        backgroundColorRanges.clear();
    }

    private Path getUnderlineShape(int index) {
        Path underlineShape = underlineShapes.get(index);
        if (underlineShape == null) {
            // add corresponding underline node (empty)
            underlineShape = new Path();
            underlineShape.setManaged(false);
            underlineShape.setStrokeWidth(0);
            underlineShape.layoutXProperty().bind(leftInset);
            underlineShape.layoutYProperty().bind(topInset);
            underlineShapes.set(index, underlineShape);
            getChildren().add(underlineShape);
        }
        return underlineShape;
    }

    /**
     * Updates the shape which renders the text underline.
     * 
     * @param text  The text node which specified the style attributes
     * @param start The index of the first character 
     * @param end   The index of the last character
     * @param index The index of the background shape
     */
    private void updateUnderline(TextExt text, int start, int end, int index) {

        Number underlineWidth = text.underlineWidthProperty().get();
        if (underlineWidth != null && underlineWidth.doubleValue() > 0) {

            Path underlineShape = getUnderlineShape(index);
            underlineShape.setStrokeWidth(underlineWidth.doubleValue());

            // get remaining CSS properties for the underline style

            Paint underlineColor = text.underlineColorProperty().get();
    
            // get the dash array - JavaFX CSS parser seems to return either a Number[] array
            // or a single value, depending on whether only one or more than one value has been
            // specified in the CSS
            Double[] underlineDashArray = null;
            Object underlineDashArrayProp = text.underlineDashArrayProperty().get();
            if (underlineDashArrayProp != null) {
                if (underlineDashArrayProp.getClass().isArray()) {
                    Number[] numberArray = (Number[]) underlineDashArrayProp;
                    underlineDashArray = new Double[numberArray.length];
                    int idx = 0;
                    for (Number d : numberArray) {
                        underlineDashArray[idx++] = (Double) d;
                    }
                } else {
                    underlineDashArray = new Double[1];
                    underlineDashArray[0] = ((Double) underlineDashArrayProp).doubleValue();
                }
            }
    
            StrokeLineCap underlineCap = text.underlineCapProperty().get();
    
            // apply style
            if (underlineColor != null) {
                underlineShape.setStroke(underlineColor);
            }
            if (underlineDashArray != null) {
                underlineShape.getStrokeDashArray().addAll(underlineDashArray);
            }
            if (underlineCap != null) {
                underlineShape.setStrokeLineCap(underlineCap);
            }

            // Set path elements
            PathElement[] shape = getUnderlineShape(start, end);
            underlineShape.getElements().setAll(shape);
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
