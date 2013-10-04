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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Bounds;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Extends Text to provide additional convenient methods and properties.
 */
public class PumpedUpText extends Text {

    private final Path selectionHighlightPath = new Path();
    private final ReadOnlyObjectWrapper<PathElement[]> caretShape = new ReadOnlyObjectWrapper<>();

    public PumpedUpText(String text) {
        super(text);

        // update selection path whenever font changes
        fontProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable fontProperty) {
                updateCaretShape();
                updateSelectionPath();
            }
        });
    }

    public ReadOnlyObjectProperty<PathElement[]> caretShapeProperty() {
        return caretShape.getReadOnlyProperty();
    }

    public void setCaretPosition(int pos) {
        setImpl_caretPosition(pos);
        updateCaretShape();
    }

    public int getCaretPosition(int pos) {
        return getImpl_caretPosition();
    }

    private void updateCaretShape() {
        caretShape.set(independentCopy().getImpl_caretShape());
    }

    public ReadOnlyObjectProperty<Bounds> selectionBoundsProperty() {
        return selectionHighlightPath.layoutBoundsProperty();
    }

    public void setSelection(int start, int end) {
        if(start < end) {
            setImpl_selectionStart(start);
            setImpl_selectionEnd(end);
        }
        else {
            setImpl_selectionStart(-1);
            setImpl_selectionEnd(-1);
        }
        updateSelectionPath();
    }

    public int getSelectionStart() {
        return getImpl_selectionStart();
    }

    public int getSelectionEnd() {
        return getImpl_selectionEnd();
    }

    private void updateSelectionPath() {
        selectionHighlightPath.getElements().setAll(independentCopy().getImpl_selectionShape());
    }

    /**
     * If this text node is a child of a TextFlow,
     * creates and returns a copy with no parent.
     * Otherwise, this node is returned.
     * This is useful to work around https://javafx-jira.kenai.com/browse/RT-32398
     */
    public PumpedUpText independentCopy() {
        if(getParent() instanceof TextFlow) {
            PumpedUpText text = new PumpedUpText(getText());
            text.setImpl_caretPosition(getImpl_caretPosition());
            text.setSelection(getSelectionStart(), getSelectionEnd());
            text.setFont(getFont());
            text.setTextOrigin(getTextOrigin());
            text.setBoundsType(getBoundsType());
            return text;
        }
        else {
            return this;
        }
    }

}
