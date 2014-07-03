package org.fxmisc.richtext.skin;

import static org.fxmisc.richtext.TwoDimensional.Bias.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.BiConsumer;

import javafx.beans.binding.Binding;
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
import javafx.scene.text.TextFlow;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.StyledText;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TwoLevelNavigator;

import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.text.PrismTextLayout;
import com.sun.javafx.text.TextLine;

class ParagraphText<S> extends TextFlow {

    private static Method mGetTextLayout;
    private static Method mGetLines;
    static {
        try {
            mGetTextLayout = TextFlow.class.getDeclaredMethod("getTextLayout");
            mGetLines = PrismTextLayout.class.getDeclaredMethod("getLines");
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
        mGetTextLayout.setAccessible(true);
        mGetLines.setAccessible(true);
    }

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
        clampedCaretPosition.addListener((obs, oldPos, newPos) -> updateCaretShape());

        Binding<Double> leftInset = EasyBind.map(insetsProperty(), ins -> ins.getLeft());
        Binding<Double> rightInset = EasyBind.map(insetsProperty(), ins -> ins.getTop());

        // selection highlight
        selectionShape.setManaged(false);
        selectionShape.setVisible(true);
        selectionShape.setFill(Color.DODGERBLUE);
        selectionShape.setStrokeWidth(0);
        selectionShape.layoutXProperty().bind(leftInset);
        selectionShape.layoutYProperty().bind(rightInset);
        getChildren().add(selectionShape);

        // caret
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

        selection.addListener((obs, old, sel) -> updateSelectionShape());
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

    Optional<HitInfo> hit(double x, int lineIndex) {
        return hit(x, getLineCenter(lineIndex));
    }

    Optional<HitInfo> hit(double x, double y) {
        TextLayout textLayout = textLayout();
        HitInfo hit = textLayout.getHitInfo((float)x, (float)y);

        if(hit.getCharIndex() == paragraph.length() - 1) {
            // might be a hit beyond the end of line, investigate
            PathElement[] elems = textLayout.getCaretShape(paragraph.length(), true, 0, 0);
            Path caret = new Path(elems);
            if(x > caret.getBoundsInLocal().getMinX()) {
                return Optional.empty();
            } else {
                return Optional.of(hit);
            }
        } else {
            return Optional.of(hit);
        }
    }

    public double getCaretOffsetX() {
        Bounds bounds = caretShape.getLayoutBounds();
        return (bounds.getMinX() + bounds.getMaxX()) / 2;
    }

    public Bounds getCaretBounds() {
        return caretShape.getBoundsInParent();
    }

    public Bounds getCaretBoundsOnScreen() {
        Bounds localBounds = caretShape.getBoundsInLocal();
        return caretShape.localToScreen(localBounds);
    }

    public Optional<Bounds> getSelectionBoundsOnScreen() {
        if(selection.get().getLength() == 0) {
            return Optional.empty();
        } else {
            Bounds localBounds = selectionShape.getBoundsInLocal();
            return Optional.of(selectionShape.localToScreen(localBounds));
        }
    }

    public int getLineCount() {
        return getLines().length;
    }

    public int currentLineIndex() {
        TextLine[] lines = getLines();
        TwoLevelNavigator navigator = new TwoLevelNavigator(() -> lines.length, i -> lines[i].getLength());
        return navigator.offsetToPosition(clampedCaretPosition.intValue(), Forward).getMajor();
    }

    private float getLineCenter(int index) {
        return getLineY(index) + getLines()[index].getBounds().getHeight() / 2;
    }

    private float getLineY(int index) {
        TextLine[] lines = getLines();
        float spacing = (float) getLineSpacing();
        float lineY = 0;
        for(int i = 0; i < index; ++i) {
            lineY += lines[i].getBounds().getHeight() + spacing;
        }
        return lineY;
    }

    private TextLayout textLayout() {
        return (TextLayout) invoke(mGetTextLayout, this);
    }

    private TextLine[] getLines() {
        return (TextLine[]) invoke(mGetLines, textLayout());
    }

    private static Object invoke(Method m, Object obj, Object... args) {
        try {
            return m.invoke(obj, args);
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateCaretShape() {
        PathElement[] shape = textLayout().getCaretShape(clampedCaretPosition.intValue(), true, 0, 0);
        caretShape.getElements().setAll(shape);
    }

    private void updateSelectionShape() {
        int start = selection.get().getStart();
        int end = selection.get().getEnd();
        PathElement[] shape = textLayout().getRange(start, end, TextLayout.TYPE_TEXT, 0, 0);
        selectionShape.getElements().setAll(shape);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        updateCaretShape();
        updateSelectionShape();
    }
}
