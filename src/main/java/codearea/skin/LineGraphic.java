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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.control.IndexRange;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import codearea.control.Line;
import codearea.control.StyledString;

import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.scene.text.TextLayout;

public class LineGraphic<S> extends TextFlow {

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

    private final Line<S> line;

    private final Path caretShape = new Path();
    private final Path selectionShape = new Path();

    private final InvalidationListener updateCaretShape = new InvalidationListener() {
        @Override
        public void invalidated(Observable observable) {
            updateCaretShape();
        }
    };

    private final InvalidationListener updateSelectionShape = new InvalidationListener() {
        @Override
        public void invalidated(Observable boundsProperty) {
            updateSelectionShape();
        }
    };

    public LineGraphic(Line<S> line, BiConsumer<Text, S> applyStyle) {
        this.line = line;

        setPrefWidth(Region.USE_COMPUTED_SIZE); // no wrapping

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
        for(StyledString<S> segment: line.getSegments()) {
            Text t = new Text(segment.toString());
            t.setTextOrigin(VPos.TOP);
            t.getStyleClass().add("text");
            applyStyle.accept(t, segment.getStyle());

            // XXX: binding selectionFill to textFill,
            // see the note at highlightTextFill
            t.impl_selectionFillProperty().bind(t.fillProperty());

            // keep the caret graphic up to date
            t.fontProperty().addListener(updateCaretShape);

            // keep the selection graphic up to date
            t.fontProperty().addListener(updateSelectionShape);

            getChildren().add(t);
        }

        // keep caret position up to date
        line.caretPositionProperty().addListener(updateCaretShape);
        updateCaretShape();

        // keep selection up to date
        line.selectionProperty().addListener(updateSelectionShape);
        updateSelectionShape();
    }

    public void dispose() {
        line.caretPositionProperty().removeListener(updateCaretShape);
        line.selectionProperty().removeListener(updateSelectionShape);
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

    public HitInfo hit(double x) {
        // compute y as the vertical center of the text flow
        Bounds bounds = getBoundsInLocal();
        double y = (bounds.getMinY() + bounds.getMaxY()) / 2;

        // hit
        HitInfo hit = textLayout().getHitInfo((float)x, (float)y);

        if(hit.getCharIndex() == line.length()) // clicked beyond the end of line
            hit.setLeading(true); // prevent going to the start of the next line

        return hit;
    }

    public double getCaretOffsetX() {
        Bounds bounds = caretShape.getLayoutBounds();
        return (bounds.getMinX() + bounds.getMaxX()) / 2;
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
        PathElement[] shape = textLayout().getCaretShape(line.getCaretPosition(), true, 0, 0);
        caretShape.getElements().setAll(shape);
    }

    private void updateSelectionShape() {
        IndexRange selection = line.getSelection();
        PathElement[] shape = textLayout().getRange(selection.getStart(), selection.getEnd(), TextLayout.TYPE_TEXT, 0, 0);
        selectionShape.getElements().setAll(shape);
    }
}
