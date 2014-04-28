/*
 * Copyright (c) 2013, Tomas Mikula. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.fxmisc.richtext.skin;

import static org.fxmisc.richtext.TwoDimensional.Bias.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.control.IndexRange;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.StyledText;
import org.fxmisc.richtext.TwoLevelNavigator;

import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.text.PrismTextLayout;
import com.sun.javafx.text.TextLine;

public class ParagraphGraphic<S> extends TextFlow {

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

    private final Paragraph<S> paragraph;
    private IndexRange selection;

    private final Path caretShape = new Path();
    private final Path selectionShape = new Path();

    public ParagraphGraphic(Paragraph<S> par, BiConsumer<Text, S> applyStyle) {
        this.paragraph = par;

        clampedCaretPosition = Bindings.min(caretPosition, paragraph.length());
        clampedCaretPosition.addListener((obs, oldPos, newPos) -> updateCaretShape());

        // selection highlight
        selectionShape.setManaged(false);
        selectionShape.setVisible(true);
        selectionShape.setFill(Color.DODGERBLUE);
        selectionShape.setStrokeWidth(0);
        getChildren().add(selectionShape);

        // caret
        caretShape.setManaged(false);
        caretShape.setStrokeWidth(1);
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

            // keep the caret graphic up to date
            t.fontProperty().addListener(obs -> updateCaretShape());

            // keep the selection graphic up to date
            t.fontProperty().addListener(obs -> updateSelectionShape());

            getChildren().add(t);
        }

        setCaretPosition(0);
        setSelection(0, 0);
    }

    public Paragraph<S> getParagraph() {
        return paragraph;
    }

    void setSelection(IndexRange selection) {
        this.selection = selection;
        updateSelectionShape();
    }

    void setSelection(int start, int end) {
        setSelection(new IndexRange(start, end));
    }

    public BooleanProperty caretVisibleProperty() {
        return caretShape.visibleProperty();
    }

    public ObjectProperty<Paint> highlightFillProperty() {
        return selectionShape.fillProperty();
    }

    HitInfo hit(int lineIndex, double x) {
        return hit(x, getLineCenter(lineIndex));
    }

    HitInfo hit(double x, double y) {
        HitInfo hit = textLayout().getHitInfo((float)x, (float)y);

        if(hit.getCharIndex() == paragraph.length()) // clicked beyond the end of line
            hit.setLeading(true); // prevent going to the start of the next line

        return hit;
    }

    public double getCaretOffsetX() {
        Bounds bounds = caretShape.getLayoutBounds();
        return (bounds.getMinX() + bounds.getMaxX()) / 2;
    }

    public Point2D getCaretLocationOnScreen() {
        Bounds bounds = caretShape.getBoundsInLocal();
        // XXX: shift 4 pixels to the right in order not to hide the caret
        return caretShape.localToScreen(bounds.getMaxX() + 4, bounds.getMinY());
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
        int start = selection.getStart();
        int end = selection.getEnd();
        PathElement[] shape = textLayout().getRange(start, end, TextLayout.TYPE_TEXT, 0, 0);
        selectionShape.getElements().setAll(shape);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        updateCaretShape();
    }
}
