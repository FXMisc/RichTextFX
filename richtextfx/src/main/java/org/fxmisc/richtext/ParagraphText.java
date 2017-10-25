package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.StrokeLineCap;

import javafx.scene.shape.StrokeType;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyledSegment;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuples;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * The class responsible for rendering the segments in an paragraph. It also renders additional RichTextFX-specific
 * CSS found in {@link TextExt} as well as the selection and caret shapes.
 *
 * @param <PS> paragraph style type
 * @param <SEG> segment type
 * @param <S> segment style type
 */
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

    private final Path caretShape = new CaretPath();
    private final Path selectionShape = new SelectionPath();

    private final CustomCssShapeHelper<Paint> backgroundShapeHelper;
    private final CustomCssShapeHelper<BorderAttributes> borderShapeHelper;
    private final CustomCssShapeHelper<UnderlineAttributes> underlineShapeHelper;

    // proxy for caretShape.visibleProperty() that implements unbind() correctly.
    // This is necessary due to a bug in BooleanPropertyBase#unbind().
    // See https://bugs.openjdk.java.net/browse/JDK-8130458
    private final Var<Boolean> caretVisible = Var.newSimpleVar(false);
    {
        caretShape.visibleProperty().bind(caretVisible);
    }

    ParagraphText(Paragraph<PS, SEG, S> par, Function<StyledSegment<SEG, S>, Node> nodeFactory) {
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
        par.getStyledSegments().stream().map(nodeFactory).forEach(n -> {
            if (n instanceof TextExt) {
                TextExt t = (TextExt) n;
                // XXX: binding selectionFill to textFill,
                // see the note at highlightTextFill
                JavaFXCompatibility.Text_selectionFillProperty(t).bind(t.fillProperty());
            }
            getChildren().add(n);
        });

        // set up custom css shape helpers
        Supplier<Path> createBackgroundShape = () -> {
            Path shape = new BackgroundPath();
            shape.setManaged(false);
            shape.layoutXProperty().bind(leftInset);
            shape.layoutYProperty().bind(topInset);
            return shape;
        };
        Supplier<Path> createBorderShape = () -> {
            Path shape = new BorderPath();
            shape.setManaged(false);
            shape.layoutXProperty().bind(leftInset);
            shape.layoutYProperty().bind(topInset);
            return shape;
        };
        Supplier<Path> createUnderlineShape = () -> {
            Path shape = new UnderlinePath();
            shape.setManaged(false);
            shape.layoutXProperty().bind(leftInset);
            shape.layoutYProperty().bind(topInset);
            return shape;
        };

        Consumer<Collection<Path>> clearUnusedShapes = paths -> getChildren().removeAll(paths);
        Consumer<Path> addToBackground = path -> getChildren().add(0, path);
        Consumer<Path> addToForeground = path -> getChildren().add(path);
        backgroundShapeHelper = new CustomCssShapeHelper<>(
                createBackgroundShape,
                (backgroundShape, tuple) -> {
                    backgroundShape.setStrokeWidth(0);
                    backgroundShape.setFill(tuple._1);
                    backgroundShape.getElements().setAll(getRangeShape(tuple._2));
                },
                addToBackground,
                clearUnusedShapes
        );
        borderShapeHelper = new CustomCssShapeHelper<>(
                createBorderShape,
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
                },
                addToBackground,
                clearUnusedShapes
        );
        underlineShapeHelper = new CustomCssShapeHelper<>(
                createUnderlineShape,
                (underlineShape, tuple) -> {
                    UnderlineAttributes attributes = tuple._1;
                    underlineShape.setStroke(attributes.color);
                    underlineShape.setStrokeWidth(attributes.width);
                    underlineShape.setStrokeLineCap(attributes.cap);
                    if (attributes.dashArray != null) {
                        underlineShape.getStrokeDashArray().setAll(attributes.dashArray);
                    }
                    underlineShape.getElements().setAll(getUnderlineShape(tuple._2));
                },
                addToForeground,
                clearUnusedShapes
        );
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
        PathElement[] rangeShape = getRangeShapeSafely(from, to);

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
        selectionShape.getElements().setAll(getRangeShapeSafely(start, end));
    }

    /**
     * Gets the range shape for the given positions within the text, including the newline character, if range
     * defined by the start/end arguments include it.
     *
     * @param start the start position of the range shape
     * @param end the end position of the range shape. If {@code end == paragraph.length() + 1}, the newline character
     *            will be included in the selection by selecting the rest of the line
     */
    private PathElement[] getRangeShapeSafely(int start, int end) {
        PathElement[] shape;
        if (end <= paragraph.length()) {
            // selection w/o newline char
            shape = getRangeShape(start, end);
        } else {
            // Selection includes a newline character.
            if (paragraph.length() == 0) {
                // empty paragraph
                shape = createRectangle(0, 0, getWidth(), getHeight());
            } else if (start == paragraph.length()) {
                // selecting only the newline char

                // calculate the bounds of the last character
                shape = getRangeShape(start - 1, start);
                LineTo lineToTopRight = (LineTo) shape[shape.length - 4];
                shape = createRectangle(lineToTopRight.getX(), lineToTopRight.getY(), getWidth(), getHeight());
            } else {
                shape = getRangeShape(start, paragraph.length());
                // Since this might be a wrapped multi-line paragraph,
                // there may be multiple groups of (1 MoveTo, 3 LineTo objects) for each line:
                // MoveTo(topLeft), LineTo(topRight), LineTo(bottomRight), LineTo(bottomLeft)

                // We only need to adjust the top right and bottom right corners to extend to the
                // width/height of the line, simulating a full line selection.
                int length = shape.length;
                int bottomRightIndex = length - 3;
                int topRightIndex = bottomRightIndex - 1;
                LineTo lineToTopRight = (LineTo) shape[topRightIndex];
                shape[topRightIndex] = new LineTo(getWidth(), lineToTopRight.getY());
                shape[bottomRightIndex] = new LineTo(getWidth(), getHeight());
            }
        }

        if (getLineCount() > 1) {
            // adjust right corners of wrapped lines
            boolean wrappedAtEndPos = (end > 0 && getLineOfCharacter(end) > getLineOfCharacter(end - 1));
            int adjustLength = shape.length - (wrappedAtEndPos ? 0 : 5);
            for (int i = 0; i < adjustLength; i++) {
                if (shape[i] instanceof MoveTo) {
                    ((LineTo)shape[i + 1]).setX(getWidth());
                    ((LineTo)shape[i + 2]).setX(getWidth());
                }
            }
        }

        return shape;
    }

    private PathElement[] createRectangle(double topLeftX, double topLeftY, double bottomRightX, double bottomRightY) {
        return new PathElement[] {
                new MoveTo(topLeftX, topLeftY),
                new LineTo(bottomRightX, topLeftY),
                new LineTo(bottomRightX, bottomRightY),
                new LineTo(topLeftX, bottomRightY),
                new LineTo(topLeftX, topLeftY)
        };
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
                backgroundShapeHelper.updateSharedShapeRange(backgroundColor, start, end);
            }

            BorderAttributes border = new BorderAttributes(text);
            if (!border.isNullValue()) {
                borderShapeHelper.updateSharedShapeRange(border, start, end);
            }

            UnderlineAttributes underline = new UnderlineAttributes(text);
            if (!underline.isNullValue()) {
                underlineShapeHelper.updateSharedShapeRange(underline, start, end);
            }

            start = end;
        }

        borderShapeHelper.updateSharedShapes();
        backgroundShapeHelper.updateSharedShapes();
        underlineShapeHelper.updateSharedShapes();
    }


    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        updateCaretShape();
        updateSelectionShape();
        updateBackgroundShapes();
    }

    private static class CustomCssShapeHelper<T> {

        private final List<Tuple2<T, IndexRange>> ranges = new LinkedList<>();
        private final List<Path> shapes = new LinkedList<>();

        private final Supplier<Path> createShape;
        private final BiConsumer<Path, Tuple2<T, IndexRange>> configureShape;
        private final Consumer<Path> addToChildren;
        private final Consumer<Collection<Path>> clearUnusedShapes;

        CustomCssShapeHelper(Supplier<Path> createShape, BiConsumer<Path, Tuple2<T, IndexRange>> configureShape,
                             Consumer<Path> addToChildren, Consumer<Collection<Path>> clearUnusedShapes) {
            this.createShape = createShape;
            this.configureShape = configureShape;
            this.addToChildren = addToChildren;
            this.clearUnusedShapes = clearUnusedShapes;
        }

        /**
         * Calculates the range of a value (background color, underline, etc.) that is shared between multiple
         * consecutive {@link TextExt} nodes
         */
        private void updateSharedShapeRange(T value, int start, int end) {
            Runnable addNewValueRange = () -> ranges.add(Tuples.t(value, new IndexRange(start, end)));

            if (ranges.isEmpty()) {
                addNewValueRange.run();;
            } else {
                int lastIndex = ranges.size() - 1;
                Tuple2<T, IndexRange> lastShapeValueRange = ranges.get(lastIndex);
                T lastShapeValue = lastShapeValueRange._1;

                // calculate smallest possible position which is consecutive to the given start position
                final int prevEndNext = lastShapeValueRange.get2().getEnd() + 1;
                if (start <= prevEndNext &&         // Consecutive?
                    lastShapeValue.equals(value)) { // Same style?

                    IndexRange lastRange = lastShapeValueRange._2;
                    IndexRange extendedRange = new IndexRange(lastRange.getStart(), end);
                    ranges.set(lastIndex, Tuples.t(lastShapeValue, extendedRange));
                } else {
                    addNewValueRange.run();
                }
            }
        }

        /**
         * Updates the shapes calculated in {@link #updateSharedShapeRange(Object, int, int)} and configures them
         * via {@code configureShape}.
         */
        private void updateSharedShapes() {
            // remove or add shapes, depending on what's needed
            int neededNumber = ranges.size();
            int availableNumber = shapes.size();

            if (neededNumber < availableNumber) {
                List<Path> unusedShapes = shapes.subList(neededNumber, availableNumber);
                clearUnusedShapes.accept(unusedShapes);
                unusedShapes.clear();
            } else if (availableNumber < neededNumber) {
                for (int i = 0; i < neededNumber - availableNumber; i++) {
                    Path shape = createShape.get();

                    shapes.add(shape);
                    addToChildren.accept(shape);
                }
            }

            // update the shape's color and elements
            for (int i = 0; i < ranges.size(); i++) {
                configureShape.accept(shapes.get(i), ranges.get(i));
            }

            // clear, since it's no longer needed
            ranges.clear();
        }
    }

    private static class BorderAttributes extends LineAttributesBase {

        final StrokeType type;

        BorderAttributes(TextExt text) {
            super(text.getBorderStrokeColor(), text.getBorderStrokeWidth(), text.borderStrokeDashArrayProperty());
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
            super(text.getUnderlineColor(), text.getUnderlineWidth(), text.underlineDashArrayProperty());
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

        /**
         * Java Quirk! Using {@code t.get[border/underline]DashArray()} throws a ClassCastException
         * "Double cannot be cast to Number". However, using {@code t.getDashArrayProperty().get()}
         * works without issue
         */
        LineAttributesBase(Paint color, Number width, ObjectProperty<Number[]> dashArrayProp) {
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
                Object dashArrayProperty = dashArrayProp.get();
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
