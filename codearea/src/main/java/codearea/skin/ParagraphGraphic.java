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

package codearea.skin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
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
import codearea.control.Paragraph;
import codearea.control.StyledText;

import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.scene.text.TextLayout;

public class ParagraphGraphic<S> extends TextFlow {

    private static Method mGetTextLayout;
    static {
        try {
            mGetTextLayout = TextFlow.class.getDeclaredMethod("getTextLayout");
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
        mGetTextLayout.setAccessible(true);
    }

    // FIXME: changing it currently has not effect, because
    // Text.impl_selectionFillProperty().set(newFill) doesn't work
    // properly for Text node inside a TextFlow (as of JDK8-b100).
    private final ObjectProperty<Paint> highlightTextFill = new SimpleObjectProperty<Paint>(Color.WHITE);

    private final Paragraph<S> paragraph;
    private int caretPosition;
    private IndexRange selection;

    private final Path caretShape = new Path();
    private final Path selectionShape = new Path();

    public ParagraphGraphic(Paragraph<S> par, BiConsumer<Text, S> applyStyle) {
        this.paragraph = par;

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

    public void dispose() {
        // do nothing
    }

    public Paragraph<S> getParagraph() {
        return paragraph;
    }

    void setCaretPosition(int pos) {
        if(pos < 0 || pos > paragraph.length())
            throw new IndexOutOfBoundsException();
        caretPosition = pos;
        updateCaretShape();
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

    public ObjectProperty<Paint> highlightTextFillProperty() {
        return highlightTextFill;
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

    private double getLineCenter(int index) {
        return verticalCenterOf(getLineSpans()[index]);
    }

    public double getCaretOffsetX() {
        Bounds bounds = caretShape.getLayoutBounds();
        return (bounds.getMinX() + bounds.getMaxX()) / 2;
    }

    public int getLineCount() {
        return getLineSpans().length;
    }

    private Bounds[] getLineSpans() {
        return getLineSpans(0, paragraph.length());
    }

    private Bounds[] getLineSpans(int minPos, int maxPos) {
        Bounds first = getLineSpanAt(minPos);
        Bounds last = getLineSpanAt(maxPos);

        if(verticalCenterOf(last) < first.getMaxY()) {
            return new Bounds[] { first };
        } else {
            int midPos = (minPos + maxPos) / 2;
            if(midPos == minPos) {
                return new Bounds[] { first, last };
            } else {
                Bounds[] left = getLineSpans(minPos, midPos);
                Bounds[] right = getLineSpans(midPos, maxPos);
                Bounds[] res = new Bounds[left.length + right.length-1];
                System.arraycopy(left, 0, res, 0, left.length);
                System.arraycopy(right, 1, res, left.length, right.length-1);
                return res;
            }
        }
    }

    private Bounds getLineSpanAt(int position) {
        Path caretAtPos = new Path(textLayout().getCaretShape(position, true, 0, 0));
        return caretAtPos.getBoundsInLocal();
    }

    private static double verticalCenterOf(Bounds bounds) {
        return (bounds.getMinY() + bounds.getMaxY()) / 2;
    }

    public int currentLineIndex() {
        return getLineSpans(0, caretPosition).length - 1;
    }

    private TextLayout textLayout() {
        try {
            return (TextLayout) mGetTextLayout.invoke(this);
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateCaretShape() {
        PathElement[] shape = textLayout().getCaretShape(caretPosition, true, 0, 0);
        caretShape.getElements().setAll(shape);
    }

    private void updateSelectionShape() {
        int start = selection.getStart();
        int end = selection.getEnd();
        PathElement[] shape = textLayout().getRange(start, end, TextLayout.TYPE_TEXT, 0, 0);
        selectionShape.getElements().setAll(shape);
    }
}
