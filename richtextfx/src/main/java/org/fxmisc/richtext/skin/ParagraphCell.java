package org.fxmisc.richtext.skin;

import java.util.Optional;
import java.util.function.BiConsumer;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicObservableValue;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.StyledTextArea;

import com.sun.javafx.scene.text.HitInfo;

public class ParagraphCell<S> extends ListCell<Paragraph<S>> {
    private final StyledTextAreaSkin<S> skin;
    private final BiConsumer<Text, S> applyStyle;
    private final InvalidationListener onWrapWidthChange = obs -> requestLayout();

    @SuppressWarnings("unchecked")
    private final MonadicObservableValue<ParagraphGraphic<S>> graphic = EasyBind.monadic(graphicProperty()).map(g -> (ParagraphGraphic<S>) g);

    private final Property<Boolean> caretVisible = graphic.selectProperty(ParagraphGraphic::caretVisibleProperty);
    public Property<Boolean> caretVisibleProperty() { return caretVisible; }

    private final Property<Paint> highlightFill = graphic.selectProperty(ParagraphGraphic::highlightFillProperty);
    public Property<Paint> highlightFillProperty() { return highlightFill; }

    private final Property<Paint> highlightTextFill = graphic.selectProperty(ParagraphGraphic::highlightTextFillProperty);
    public Property<Paint> highlightTextFillProperty() { return highlightTextFill; }

    public ParagraphCell(StyledTextAreaSkin<S> skin, BiConsumer<Text, S> applyStyle) {
        this.skin = skin;
        this.applyStyle = applyStyle;

        emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
            if(wasEmpty && !isEmpty) {
                startListening();
            } else if(!wasEmpty && isEmpty) {
                stopListening();
            }
        });
    }

    @Override
    protected void updateItem(Paragraph<S> item, boolean empty) {
        super.updateItem(item, empty);

        if(!empty) {
            ParagraphGraphic<S> graphic = new ParagraphGraphic<S>(item, applyStyle);

            StyledTextArea<S> area = skin.getSkinnable();
            if(getIndex() == area.getCurrentParagraph()) {
                int col = Math.min(area.getCaretColumn(), item.length());
                graphic.setCaretPosition(col);
            }

            graphic.setSelection(area.getParagraphSelection(getIndex()));

            setGraphic(graphic);
        } else {
            setGraphic(null);
        }
    }

    @Override
    protected double computePrefHeight(double width) {
        // XXX we cannot rely on the given width, because ListView does not pass
        // the correct width (https://javafx-jira.kenai.com/browse/RT-35041)
        // So we have to get the width by our own means.
        width = getWrapWidth();

        if(isEmpty()) {
            // go big so that we don't need to construct too many empty cells
            return 200;
        } else {
            return getParagraphGraphic().prefHeight(width) + snappedTopInset() + snappedBottomInset();
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        if(isEmpty()) {
            return super.computePrefWidth(height);
        } else if(getWrapWidth() == Region.USE_COMPUTED_SIZE) {
                return getParagraphGraphic().prefWidth(-1.0) + snappedLeftInset() + snappedRightInset();
        } else {
            return 0;
        }
    }

    private double getWrapWidth() {
        double skinWrapWidth = skin.wrapWidth.get();
        if(skinWrapWidth == Region.USE_COMPUTED_SIZE) {
            return Region.USE_COMPUTED_SIZE;
        } else {
            return skinWrapWidth - snappedLeftInset() - snappedRightInset();
        }
    }

    private void startListening() {
        skin.wrapWidth.addListener(onWrapWidthChange);
    }

    private void stopListening() {
        skin.wrapWidth.removeListener(onWrapWidthChange);
    }

    /**
     * Returns a HitInfo for the given mouse event.
     * The returned character index is an index within the whole text content
     * of the code area, not relative to this cell.
     *
     * If this cell is empty, then the position at the end of text content
     * is returned.
     */
    public HitInfo hit(MouseEvent e) {
        if(isEmpty()) { // hit beyond the last line
            return hitEnd();
        } else {
            ParagraphGraphic<S> textFlow = getParagraphGraphic();
            HitInfo hit = textFlow.hit(e.getX() - textFlow.getLayoutX(), e.getY());
            return toGlobalHit(hit);
        }
    }

    /**
     * Hits the embedded TextFlow at the given line and x offset.
     * The returned character index is an index within the whole text
     * content of the code area rather than relative to this cell.
     *
     * If this cell is empty, then the position at the end of text content
     * is returned.
     */
    HitInfo hit(int line, double x) {
        // obtain HitInfo relative to this paragraph
        HitInfo hit = getParagraphGraphic().hit(line, x);

        // add paragraph offset
        return toGlobalHit(hit);
    }

    private HitInfo toGlobalHit(HitInfo hit) {
        // add paragraph offset
        int parOffset = skin.getSkinnable().position(getIndex(), 0).toOffset();
        hit.setCharIndex(parOffset + hit.getCharIndex());

        return hit;
    }

    private HitInfo hitEnd() {
        HitInfo hit = new HitInfo();
        hit.setCharIndex(skin.getSkinnable().getLength());
        hit.setLeading(true);
        return hit;
    }

    public double getCaretOffsetX() {
        ParagraphGraphic<S> graphic = getParagraphGraphic();
        return graphic != null ? graphic.getCaretOffsetX() : 0;
    }

    ParagraphGraphic<S> getParagraphGraphic() {
        Optional<ParagraphGraphic<S>> graphic = tryGetParagraphGraphic();
        if(graphic.isPresent()) {
            return graphic.get();
        } else {
            throw new AssertionError("There's no graphic in this cell");
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<ParagraphGraphic<S>> tryGetParagraphGraphic() {
        Node graphic = getGraphic();
        if(graphic != null) {
            return Optional.of((ParagraphGraphic<S>) graphic);
        } else {
            return Optional.empty();
        }
    }

    public int getLineCount() {
        return getParagraphGraphic().getLineCount();
    }

    public int getCurrentLineIndex() {
        return getParagraphGraphic().currentLineIndex();
    }
}