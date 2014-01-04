/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates and Tomas Mikula.
 * All rights reserved.
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

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.Duration;
import codearea.behavior.CodeAreaBehavior;
import codearea.control.Paragraph;
import codearea.control.StyledTextAreaBase;
import codearea.control.TwoDimensional.Position;
import codearea.control.TwoLevelNavigator;
import codearea.skin.CssProperties.HighlightFillProperty;
import codearea.skin.CssProperties.HighlightTextFillProperty;

import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import com.sun.javafx.scene.text.HitInfo;

/**
 * Code area skin.
 */
public class StyledTextAreaSkin<S> extends BehaviorSkinBase<StyledTextAreaBase<S>, CodeAreaBehavior<S>> {

    /**
     * Background fill for highlighted text.
     */
    final StyleableObjectProperty<Paint> highlightFill
            = new HighlightFillProperty(this, Color.DODGERBLUE);

    /**
     * Text color for highlighted text.
     */
    final StyleableObjectProperty<Paint> highlightTextFill
            = new HighlightTextFillProperty(this, Color.WHITE);

    private final MyListView<Paragraph<S>> listView;
    final DoubleProperty wrapWidth = new SimpleDoubleProperty(this, "wrapWidth");
    private void updateWrapWidth() {
        if(getSkinnable().isWrapText()) {
            wrapWidth.bind(listView.widthProperty());
        } else {
            wrapWidth.unbind();
            wrapWidth.set(Double.MAX_VALUE); // no wrapping
        }
    }

    private final BooleanPulse caretPulse = new BooleanPulse(Duration.seconds(.5));
    final ObservableBooleanValue caretVisible;

    // keeps track of the currently selected cell,
    // i.e. the cell with the caret
    private ParagraphCell<S> selectedCell = null;

    // used for two-level navigation, where on the higher level are
    // paragraphs and on the lower level are lines within a paragraph
    private final TwoLevelNavigator navigator;

    public StyledTextAreaSkin(final StyledTextAreaBase<S> styledTextArea, BiConsumer<Text, S> applyStyle) {
        super(styledTextArea, new CodeAreaBehavior<S>(styledTextArea));
        getBehavior().setCodeAreaSkin(this);

        // initialize navigator
        IntSupplier cellCount = () -> getSkinnable().getParagraphs().size();
        IntUnaryOperator cellLength = i -> getCell(i).getLineCount();
        navigator = new TwoLevelNavigator(cellCount, cellLength);

        // load the default style
        styledTextArea.getStylesheets().add(StyledTextAreaSkin.class.getResource("styled-text-area.css").toExternalForm());

        // Initialize content
        listView = new MyListView<Paragraph<S>>(styledTextArea.getParagraphs());
        getChildren().add(listView);

        // Use LineCell as cell implementation
        listView.setCellFactory(new Callback<ListView<Paragraph<S>>, ListCell<Paragraph<S>>>() {
            @Override
            public ListCell<Paragraph<S>> call(final ListView<Paragraph<S>> listView) {
                final ParagraphCell<S> cell = new ParagraphCell<S>(StyledTextAreaSkin.this, applyStyle);

                cell.selectedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> o, Boolean old, Boolean selected) {
                        if(selected)
                            selectedCell = cell;
                    }
                });

                // listen to mouse events on lines
                cell.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent event) {
                        getBehavior().mousePressed(event);
                        event.consume();
                    }
                });
                cell.addEventHandler(MouseEvent.DRAG_DETECTED, new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        // startFullDrag() causes subsequent drag events to be
                        // received by corresponding LineCells, instead of all
                        // events being delivered to the original LineCell.
                        cell.getScene().startFullDrag();
                        getBehavior().dragDetected(event);
                        event.consume();
                    }
                });
                cell.addEventHandler(MouseDragEvent.MOUSE_DRAG_OVER, new EventHandler<MouseDragEvent>() {
                    @Override public void handle(MouseDragEvent event) {
                        getBehavior().mouseDragOver(event);
                        event.consume();
                    }
                });
                cell.addEventHandler(MouseDragEvent.MOUSE_DRAG_RELEASED, new EventHandler<MouseDragEvent>() {
                    @Override public void handle(MouseDragEvent event) {
                        getBehavior().mouseDragReleased(event);
                        event.consume();
                    }
                });
                cell.addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent event) {
                        getBehavior().mouseReleased(event);
                        event.consume();
                    }
                });

                return cell;
            }
        });

        // make wrapWidth behave according to the wrapText property
        styledTextArea.wrapTextProperty().addListener(o -> updateWrapWidth());
        updateWrapWidth();

        // selected line reflects the caret row
        styledTextArea.currentParagraph().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                    Number oldRow, Number currentRow) {
                listView.getSelectionModel().select(currentRow.intValue());
                listView.show(currentRow.intValue());
            }
        });
        listView.getSelectionModel().select(styledTextArea.getCurrentParagraph());

        // If the current line changes without changing the current row
        // we need to reselect the corresponding list item.
        // We rely on the fact that this listener is called _after_
        // listViews listener on the same list. For this reason, we cannot
        // use InvalidationListener, since that one would be called before
        // change listeners.
        listView.getItems().addListener(new ListChangeListener<Paragraph<S>>() {
            @Override
            public void onChanged(
                    javafx.collections.ListChangeListener.Change<? extends Paragraph<S>> arg0) {
                listView.getSelectionModel().select(styledTextArea.getCurrentParagraph());
            }
        });

        // blink caret when focused
        styledTextArea.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean wasFocused, Boolean focused) {
                if(focused)
                    caretPulse.start(true);
                else
                    caretPulse.stop(false);
            }
        });
        if (styledTextArea.isFocused())
            caretPulse.start(true);

        // The caret is visible when code area is not disabled, focused and editable.
        caretVisible = new BooleanBinding() {
            { bind(styledTextArea.focusedProperty(), styledTextArea.disabledProperty(),
                    styledTextArea.editableProperty(), caretPulse);}
            @Override protected boolean computeValue() {
                return caretPulse.get() &&
                        styledTextArea.isFocused() &&
                        !styledTextArea.isDisabled() &&
                        styledTextArea.isEditable();
            }
        };
    }

    public void showAsFirst(int index) {
        listView.showAsFirst(index);
    }

    public void showAsLast(int index) {
        listView.showAsLast(index);
    }

    public int getFirstVisibleIndex() {
        return listView.getFirstVisibleIndex();
    }

    public int getLastVisibleIndex() {
        return listView.getLastVisibleIndex();
    }

    public double getCaretOffsetX() {
        return selectedCell != null ? selectedCell.getCaretOffsetX() : 0;
    }

    public HitInfo hit(Position targetLine, double x) {
        return getCell(targetLine.getMajor()).hit(targetLine.getMinor(), x);
    }

    /**
     * Returns the current line as a two-level index.
     * The major number is the paragraph index, the minor
     * number is the line number within the paragraph.
     */
    public Position currentLine() {
        int parIdx = getSkinnable().getCurrentParagraph();
        int lineIdx = getCell(parIdx).getCurrentLineIndex();

        return position(parIdx, lineIdx);
    }

    public Position position(int par, int line) {
        return navigator.position(par, line);
    }

    @Override
    public void dispose() {
        // TODO Unregister listeners on text editor, line list
        throw new UnsupportedOperationException();
    }

    private ParagraphCell<S> getCell(int index) {
        return (ParagraphCell<S>) listView.getCell(index);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return Arrays.<CssMetaData<? extends Styleable, ?>>asList(
                highlightFill.getCssMetaData(),
                highlightTextFill.getCssMetaData());
    }
}