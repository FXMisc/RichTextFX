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

import java.util.Optional;
import java.util.function.BiConsumer;

import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.StyledTextArea;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

import com.sun.javafx.Utils;
import com.sun.javafx.scene.text.HitInfo;

public class ParagraphCell<S> extends ListCell<Paragraph<S>> {

    private final StyledTextAreaSkin<S> skin;

    private final ObservableBooleanValue caretVisible;

    private final BiConsumer<Text, S> applyStyle;

    public ParagraphCell(StyledTextAreaSkin<S> skin, BiConsumer<Text, S> applyStyle) {
        this.skin = skin;
        this.applyStyle = applyStyle;

        // Caret is visible only on the selected line,
        // but only if the owner StyledTextArea has visible caret.
        caretVisible = Bindings.and(this.selectedProperty(), skin.caretVisible);
    }

    @Override
    protected void updateItem(Paragraph<S> item, boolean empty) {
        super.updateItem(item, empty);

        // dispose old ParagraphGraphic (unregister listeners etc.)
        tryGetParagraphGraphic().ifPresent(oldGraphic -> {
            oldGraphic.caretVisibleProperty().unbind();
            oldGraphic.highlightFillProperty().unbind();
            oldGraphic.highlightTextFillProperty().unbind();
            oldGraphic.dispose();
        });

        if(!empty) {
            ParagraphGraphic<S> graphic = new ParagraphGraphic<S>(item, applyStyle);
            graphic.caretVisibleProperty().bind(caretVisible);
            graphic.highlightFillProperty().bind(skin.highlightFill);
            graphic.highlightTextFillProperty().bind(skin.highlightTextFill);

            StyledTextArea<S> area = skin.getSkinnable();
            if(getIndex() == area.getCurrentParagraph()) {
                int col = Utils.clamp(0, area.getCaretColumn(), item.length());
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
        } else if(skin.wrapWidth.get() == Region.USE_COMPUTED_SIZE) {
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