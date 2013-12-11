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

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import codearea.control.Line;

import com.sun.javafx.scene.text.HitInfo;

public class LineCell extends ListCell<Line> {

    private final StyledTextAreaSkin skin;

    private final ObservableBooleanValue caretVisible;

    public LineCell(StyledTextAreaSkin skin) {
        this.skin = skin;

        // Caret is visible only on the selected line,
        // but only if the owner CodeArea has visible caret.
        caretVisible = Bindings.and(this.selectedProperty(), skin.caretVisible);
    }

    @Override
    protected void updateItem(Line item, boolean empty) {
        super.updateItem(item, empty);

        // dispose old LineNode (unregister listeners etc.)
        LineGraphic oldLineGraphic = (LineGraphic) getGraphic();
        if(oldLineGraphic != null) {
            oldLineGraphic.caretVisibleProperty().unbind();
            oldLineGraphic.highlightFillProperty().unbind();
            oldLineGraphic.highlightTextFillProperty().unbind();
            oldLineGraphic.dispose();
        }

        if(!empty) {
            LineGraphic lineGraphic = new LineGraphic(item);
            lineGraphic.caretVisibleProperty().bind(caretVisible);

            // highlightFill and highlightTextFill are taken from the skin
            lineGraphic.highlightFillProperty().bind(skin.highlightFill);
            lineGraphic.highlightTextFillProperty().bind(skin.highlightTextFill);

            setGraphic(lineGraphic);
        }
        else {
            setGraphic(null);
            if(getGraphic() != null)
                throw new AssertionError();
        }
    }

    /**
     * Returns a HitInfo for the given mouse event.
     * The returned character index is an index within the whole text content
     * of the code area, not relative to this cell.
     *
     * If this cell is empty, then the position at the end of text content
     * is returned.
     *
     * @param e
     */
    public HitInfo hit(MouseEvent e) {
        if(isEmpty()) { // hit beyond the last line
            HitInfo hit = new HitInfo();
            hit.setCharIndex(skin.getSkinnable().getLength());
            hit.setLeading(true);
            return hit;
        }

        // get hit in the clicked line
        LineGraphic lineGraphic = (LineGraphic) getGraphic();
        HitInfo hit = lineGraphic.hit(e.getX() - lineGraphic.getLayoutX());

        // add line offset
        hit.setCharIndex(skin.getSkinnable().getLineOffset(getIndex()) + hit.getCharIndex());

        return hit;
    }
}