package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
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

import javafx.scene.shape.StrokeType;
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
    private final List<Path> underlineShapes = new LinkedList<>();
    private final List<Path> borderShapes = new LinkedList<>();

    private final List<Tuple2<Paint, IndexRange>> backgroundColorRanges = new LinkedList<>();
    private final List<Tuple2<UnderlineAttributes, IndexRange>> underlineRanges = new LinkedList<>();
    private final List<Tuple2<BorderAttributes, IndexRange>> borderRanges = new LinkedList<>();
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

        // populate with text nodes
        for(SEG segment: par.getSegments()) {
            // create Segment
            Node fxNode = nodeFactory.apply(segment);
            getChildren().add(fxNode);
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
        int start = 0;

        // calculate shared values among consecutive nodes
        FilteredList<Node> nodeList = getChildren().filtered(node -> node instanceof TextExt);
        for (Node node : nodeList) {
            TextExt text = (TextExt) node;
            int end = start + text.getText().length();

            Paint backgroundColor = text.getBackgroundColor();
            if (backgroundColor != null) {
                updateSharedShapeRange(backgroundColorRanges, backgroundColor, start, end);
            }

            BorderAttributes border = new BorderAttributes(text);
            if (!border.isNullValue()) {
                updateSharedShapeRange(borderRanges, border, start, end);
            }

            UnderlineAttributes underline = new UnderlineAttributes(text);
            if (!underline.isNullValue()) {
                updateSharedShapeRange(underlineRanges, underline, start, end);
            }

            start = end;
        }

        // now only use one shape per shared value
        BiConsumer<ObservableList<Node>, Path> addToBackground = (children, shape) -> children.add(0, shape);
        BiConsumer<ObservableList<Node>, Path> addToForeground = ObservableList::add;

        // | background color -> border -> text -> underline -> user's eye
        updateSharedShapes(borderRanges, borderShapes, addToBackground,
                (borderShape, tuple) -> {
                    BorderAttributes attributes = tuple._1;
                    borderShape.setStrokeWidth(attributes.width);
                    borderShape.setStroke(attributes.color);
                    if (attributes.type != null) {
                        borderShape.setStrokeType(attributes.type);
                    }
                    if (attributes.dashArray != null) {
                        borderShape.getStrokeDashArray().setAll(attributes.dashArray);
                    }
                    borderShape.getElements().setAll(getRangeShape(tuple._2));
                });
        updateSharedShapes(backgroundColorRanges, backgroundShapes, addToBackground,
                (colorShape, tuple) -> {
                    colorShape.setStrokeWidth(0);
                    colorShape.setFill(tuple._1);
                    colorShape.getElements().setAll(getRangeShape(tuple._2));
        });
        updateSharedShapes(underlineRanges, underlineShapes, addToForeground,
                (underlineShape, tuple) -> {
                    UnderlineAttributes attributes = tuple._1;
                    underlineShape.setStroke(attributes.color);
                    underlineShape.setStrokeWidth(attributes.width);
                    underlineShape.setStrokeLineCap(attributes.cap);
                    if (attributes.dashArray != null) {
                        underlineShape.getStrokeDashArray().setAll(attributes.dashArray);
                    }
                    underlineShape.getElements().setAll(getUnderlineShape(tuple._2));
        });
    }

    /**
     * Calculates the range of a value (background color, underline, etc.) that is shared between multiple
     * consecutive {@link TextExt} nodes
     */
    private <T> void updateSharedShapeRange(List<Tuple2<T, IndexRange>> rangeList, T value, int start, int end) {
        updateSharedShapeRange0(
                rangeList,
                () -> Tuples.t(value, new IndexRange(start, end)),
                lastRange -> {
                    T lastShapeValue = lastRange._1;
                    return lastShapeValue.equals(value);
                },
                lastRange -> lastRange.map((val, range) -> Tuples.t(val, new IndexRange(range.getStart(), end)))
        );
    }

    private <T> void updateSharedShapeRange0(List<T> rangeList, Supplier<T> newValueRange,
                                                Predicate<T> sharesShapeValue, UnaryOperator<T> mapper) {
        if (rangeList.isEmpty()) {
            rangeList.add(newValueRange.get());
        } else {
            int lastIndex = rangeList.size() - 1;
            T lastShapeValueRange = rangeList.get(lastIndex);
            if (sharesShapeValue.test(lastShapeValueRange)) {
                rangeList.set(lastIndex, mapper.apply(lastShapeValueRange));
            } else {
                rangeList.add(newValueRange.get());
            }
        }
    }

    /**
     * Updates the shapes calculated in {@link #updateSharedShapeRange(List, Object, int, int)} and configures them
     * via {@code configureShape}.
     */
    private <T> void updateSharedShapes(List<T> rangeList, List<Path> shapeList,
                                        BiConsumer<ObservableList<Node>, Path> addToChildren,
                                        BiConsumer<Path, T> configureShape) {
        // remove or add shapes, depending on what's needed
        int neededNumber = rangeList.size();
        int availableNumber = shapeList.size();

        if (neededNumber < availableNumber) {
            List<Path> unusedShapes = shapeList.subList(neededNumber, availableNumber);
            getChildren().removeAll(unusedShapes);
            unusedShapes.clear();
        } else if (availableNumber < neededNumber) {
            for (int i = 0; i < neededNumber - availableNumber; i++) {
                Path shape = new Path();
                shape.setManaged(false);
                shape.layoutXProperty().bind(leftInset);
                shape.layoutYProperty().bind(topInset);

                shapeList.add(shape);
                addToChildren.accept(getChildren(), shape);
            }
        }

        // update the shape's color and elements
        for (int i = 0; i < rangeList.size(); i++) {
            configureShape.accept(shapeList.get(i), rangeList.get(i));
        }

        // clear, since it's no longer needed
        rangeList.clear();
    }


    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        updateCaretShape();
        updateSelectionShape();
        updateBackgroundShapes();
    }

    private static class BorderAttributes extends LineAttributesBase {

        final StrokeType type;

        BorderAttributes(TextExt text) {
            super(text.getBorderStrokeColor(), text.getBorderStrokeWidth(), text.getBorderStrokeDashArray());
            type = text.getBorderStrokeType();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BorderAttributes) {
                BorderAttributes attributes = (BorderAttributes) obj;
                return super.equals(attributes) && Objects.equals(type, attributes.type);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return String.format("BorderAttributes[type=%s %s]", type, getSubString());
        }
    }

    private static class UnderlineAttributes extends LineAttributesBase {

        final StrokeLineCap cap;

        UnderlineAttributes(TextExt text) {
            super(text.getUnderlineColor(), text.getUnderlineWidth(), text.getUnderlineDashArray());
            cap = text.getUnderlineCap();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof UnderlineAttributes) {
                UnderlineAttributes attr = (UnderlineAttributes) obj;
                return super.equals(attr) && Objects.equals(cap, attr.cap);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return String.format("UnderlineAttributes[cap=%s %s]", cap, getSubString());
        }
    }

    private static class LineAttributesBase {

        final double width;
        final Paint color;
        final Double[] dashArray;

        public final boolean isNullValue() { return color == null || width == -1; }

        LineAttributesBase(Paint color, Number width, Object dashArrayProperty) {
            this.color = color;
            if (color == null || width == null || width.doubleValue() <= 0) {
                // null value
                this.width = -1;
                dashArray = null;
            } else {
                // real value
                this.width = width.doubleValue();

                // get the dash array - JavaFX CSS parser seems to return either a Number[] array
                // or a single value, depending on whether only one or more than one value has been
                // specified in the CSS
                if (dashArrayProperty != null) {
                    if (dashArrayProperty.getClass().isArray()) {
                        Number[] numberArray = (Number[]) dashArrayProperty;
                        dashArray = new Double[numberArray.length];
                        int idx = 0;
                        for (Number d : numberArray) {
                            dashArray[idx++] = (Double) d;
                        }
                    } else {
                        dashArray = new Double[1];
                        dashArray[0] = ((Double) dashArrayProperty).doubleValue();
                    }
                } else {
                    dashArray = null;
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof UnderlineAttributes) {
                UnderlineAttributes attr = (UnderlineAttributes) obj;
                return Objects.equals(width, attr.width)
                        && Objects.equals(color, attr.color)
                        && Arrays.equals(dashArray, attr.dashArray);
            } else {
                return false;
            }
        }

        protected final String getSubString() {
            return String.format("width=%s color=%s dashArray=%s", width, color, Arrays.toString(dashArray));
        }
    }
}
