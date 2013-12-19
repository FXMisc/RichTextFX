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

import java.util.Optional;
import java.util.function.BiConsumer;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import codearea.control.Line;

import com.sun.javafx.scene.text.HitInfo;

public class LineCell<S> extends ListCell<Line<S>> {

    private final StyledTextAreaSkin<S> skin;

    private final ObservableBooleanValue caretVisible;

    private final BiConsumer<Text, S> applyStyle;

    public LineCell(StyledTextAreaSkin<S> skin, BiConsumer<Text, S> applyStyle) {
        this.skin = skin;
        this.applyStyle = applyStyle;

        // Caret is visible only on the selected line,
        // but only if the owner StyledTextArea has visible caret.
        caretVisible = Bindings.and(this.selectedProperty(), skin.caretVisible);
    }

    @Override
    protected void updateItem(Line<S> item, boolean empty) {
        super.updateItem(item, empty);

        // dispose old LineNode (unregister listeners etc.)
        Optional<LineGraphic<S>> oldGraphicOpt = tryGetLineGraphic();
        if(oldGraphicOpt.isPresent()) {
            LineGraphic<S> oldGraphic = oldGraphicOpt.get();
            oldGraphic.caretVisibleProperty().unbind();
            oldGraphic.highlightFillProperty().unbind();
            oldGraphic.highlightTextFillProperty().unbind();
            oldGraphic.dispose();
        }

        if(!empty) {
            LineGraphic<S> lineGraphic = new LineGraphic<S>(item, applyStyle);
            lineGraphic.caretVisibleProperty().bind(caretVisible);
            lineGraphic.highlightFillProperty().bind(skin.highlightFill);
            lineGraphic.highlightTextFillProperty().bind(skin.highlightTextFill);

            lineGraphic.setPrefWidth(0);

            setGraphic(lineGraphic);
        }
        else {
            setGraphic(null);
            if(getGraphic() != null)
                throw new AssertionError();
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
            return getLineGraphic().prefHeight(width) + snappedTopInset() + snappedBottomInset();
        }
    }

    private double getWrapWidth() {
        return skin.wrapWidth.get() - snappedLeftInset() - snappedRightInset();
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
            LineGraphic<S> textFlow = getLineGraphic();
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
    HitInfo hit(int visualLine, double x) {
        // obtain HitInfo relative to this line
        HitInfo hit = getLineGraphic().hit(visualLine, x);

        // add line offset
        return toGlobalHit(hit);
    }

    private HitInfo toGlobalHit(HitInfo hit) {
        // add line offset
        hit.setCharIndex(skin.getSkinnable().getLineOffset(getIndex()) + hit.getCharIndex());

        return hit;
    }

    private HitInfo hitEnd() {
        HitInfo hit = new HitInfo();
        hit.setCharIndex(skin.getSkinnable().getLength());
        hit.setLeading(true);
        return hit;
    }

    public double getCaretOffsetX() {
        LineGraphic<S> graphic = getLineGraphic();
        return graphic != null ? graphic.getCaretOffsetX() : 0;
    }

    private LineGraphic<S> getLineGraphic() {
        Optional<LineGraphic<S>> graphic = tryGetLineGraphic();
        if(graphic.isPresent()) {
            return graphic.get();
        } else {
            throw new AssertionError("There's no graphic in this cell");
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<LineGraphic<S>> tryGetLineGraphic() {
        Node graphic = getGraphic();
        if(graphic != null) {
            return Optional.of((LineGraphic<S>) graphic);
        } else {
            return Optional.empty();
        }
    }

    public int getLineCount() {
        return getLineGraphic().getLineCount();
    }

    public int getCurrentLineIndex() {
        return getLineGraphic().currentLineIndex();
    }
}