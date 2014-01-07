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

package codearea.control;

import static codearea.control.CssProperties.PSEUDO_CLASS_READONLY;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Skin;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import codearea.control.CssProperties.FontProperty;
import codearea.rx.Source;
import codearea.skin.StyledTextAreaSkin;
import codearea.undo.UndoManager;
import codearea.undo.UndoManagerProvider;
import codearea.undo.impl.ObservingUndoManager;

import com.sun.javafx.Utils;

/**
 * Text input component suitable for source code editing.
 * It allows a user to enter plain text, which can then be styled
 * programmatically (i.e. automatically), e.g. to highlight syntax.
 *
 * Subclassing is allowed to define the type of style, e.g. inline
 * style or style classes.
 *
 * @param <S> type of style that can be applied to text.
 */
public class StyledTextArea<S> extends StyledTextAreaBase<S> {

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true) {
        @Override protected void invalidated() {
            pseudoClassStateChanged(PSEUDO_CLASS_READONLY, ! get());
        }
    };
    @Override
    public final boolean isEditable() { return editable.get(); }
    @Override
    public final void setEditable(boolean value) { editable.set(value); }
    @Override
    public final BooleanProperty editableProperty() { return editable; }

    /**
     * When a run of text exceeds the width of the text region,
     * then this property indicates whether the text should wrap
     * onto another line.
     */
    private final BooleanProperty wrapText = new SimpleBooleanProperty(this, "wrapText");
    @Override
    public final boolean isWrapText() { return wrapText.get(); }
    @Override
    public final void setWrapText(boolean value) { wrapText.set(value); }
    @Override
    public final BooleanProperty wrapTextProperty() { return wrapText; }

    /**
     * The default font to use where font is not specified otherwise.
     */
    private final StyleableObjectProperty<Font> font = new FontProperty<>(this);
    public final StyleableObjectProperty<Font> fontProperty() { return font; }
    public final void setFont(Font value) { font.setValue(value); }
    public final Font getFont() { return font.getValue(); }

    /**
     * The textual content of this TextInputControl.
     */
    private final StyledTextAreaContent<S> content;
    @Override public final String getText() { return content.get(); }
    @Override
    public final String getText(int start, int end) { return content.get(start, end); }
    @Override
    public final ObservableStringValue textProperty() { return content; }

    /**
     * Stream of text changes.
     */
    public final Source<TextChange> textChanges() { return content.textChanges(); }

    /**
     * Return undo manager that can be used to undo/redo changes
     * of this text area's content.
     */
    @Override
    public UndoManager getUndoManager() { return undoManager; }
    @Override
    public void setUndoManager(UndoManagerProvider<TextChange> undoManagerFactory) {
        throw new UnsupportedOperationException();
    }
    private final UndoManager undoManager;

    /**
     * The number of characters in the text input.
     */
    @Override public final int getLength() { return content.length().get(); }
    public final ObservableIntegerValue lengthProperty() { return content.length(); }

    /**
     * The <code>anchor</code> of the text selection.
     * The <code>anchor</code> and <code>caretPosition</code> make up the selection
     * range. Selection must always be specified in terms of begin &lt;= end, but
     * <code>anchor</code> may be less than, equal to, or greater than the
     * <code>caretPosition</code>. Depending on how the user selects text,
     * the anchor might represent the lower or upper bound of the selection.
     */
    private final ReadOnlyIntegerWrapper anchor = new ReadOnlyIntegerWrapper(this, "anchor", 0);
    @Override public final int getAnchor() { return anchor.get(); }
    public final ReadOnlyIntegerProperty anchorProperty() { return anchor.getReadOnlyProperty(); }

    /**
     * The current position of the caret within the text.
     * The <code>anchor</code> and <code>caretPosition</code> make up the selection
     * range. Selection must always be specified in terms of begin &lt;= end, but
     * <code>anchor</code> may be less than, equal to, or greater than the
     * <code>caretPosition</code>. Depending on how the user selects text,
     * the caretPosition might represent the lower or upper bound of the selection.
     */
    private final ReadOnlyIntegerWrapper caretPosition = new ReadOnlyIntegerWrapper(this, "caretPosition", 0);
    @Override public final int getCaretPosition() { return caretPosition.get(); }
    public final ReadOnlyIntegerProperty caretPositionProperty() { return caretPosition.getReadOnlyProperty(); }

    /**
     * The current selection.
     */
    private final ReadOnlyObjectWrapper<IndexRange> selection = new ReadOnlyObjectWrapper<IndexRange>(this, "selection", new IndexRange(0, 0));
    @Override public final IndexRange getSelection() { return selection.getValue(); }
    public final ReadOnlyObjectProperty<IndexRange> selectionProperty() { return selection.getReadOnlyProperty(); }
    {
        selection.addListener(new ChangeListener<IndexRange>() {
            @Override
            public void changed(ObservableValue<? extends IndexRange> observable, IndexRange oldRange, IndexRange newRange) {
                int start = newRange.getStart();
                int end = newRange.getEnd();
                for (Paragraph<S> line: content.paragraphs) {
                    int lineLen = line.length();
                    if (end > start && start < lineLen) {
                        line.setSelection(start, Math.min(end, lineLen));
                    } else {
                        line.setSelection(0, 0);
                    }
                    start = Math.max(0, start - (lineLen+1));
                    end   = Math.max(0, end   - (lineLen+1));
                }
            }
        });
    }

    /**
     * Defines the characters in the TextInputControl which are selected
     */
    private final ReadOnlyStringWrapper selectedText = new ReadOnlyStringWrapper(this, "selectedText");
    @Override public final String getSelectedText() { return selectedText.get(); }
    public final ReadOnlyStringProperty selectedTextProperty() { return selectedText.getReadOnlyProperty(); }

    // current paragraph index
    private final ObservableIntegerValue caretRow;
    @Override
    public final int getCurrentParagraph() { return caretRow.get(); }
    @Override
    public final ObservableIntegerValue currentParagraph() { return caretRow; }

    /**
     * Caret position relative to the current row.
     */
    public final ObservableIntegerValue caretCol;
    @Override
    public final int getCaretColumn() { return caretCol.get(); }

    /**
     * Style used by default when no other style is provided.
     */
    private final S initialStyle;

    /**
     * Style applicator used by the default skin.
     */
    private final BiConsumer<Text, S> applyStyle;


    /**
     * Creates a text area with empty text content.
     *
     * @param applyStyle function that, given a {@link Text} node and
     * a style, applies the style to the text node. This function is
     * used by the default skin to apply style to text nodes.
     */
    public StyledTextArea(S initialStyle, BiConsumer<Text, S> applyStyle) {
        this.initialStyle = initialStyle;
        this.applyStyle = applyStyle;
        content = new StyledTextAreaContent<>(initialStyle);

        undoManager = new ObservingUndoManager<TextChange>(
                change -> replaceText(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted()), // redo lambda
                change -> replaceText(change.getPosition(), change.getPosition() + change.getInserted().length(), change.getRemoved()), // undo lambda
                (change1, change2) -> change1.mergeWith(change2), // merge lambda
                textChanges());

        ObservableValue<Position> caretPosition2D = new ObjectBinding<Position>() {
            { bind(caretPosition); }

            @Override
            protected Position computeValue() {
                return content.offsetToPosition(caretPosition.get());
            }
        };

        caretRow = new IntegerBinding() {
            { bind(caretPosition2D); }

            @Override
            protected int computeValue() {
                return caretPosition2D.getValue().getMajor();
            }
        };

        caretCol = new IntegerBinding() {
            { bind(caretPosition2D); }

            @Override
            protected int computeValue() {
                return caretPosition2D.getValue().getMinor();
            }
        };

        // The line with the caret.
        ObservableObjectValue<Paragraph<S>> currentLine = new ObjectBinding<Paragraph<S>>() {
            { bind(caretRow, content.paragraphs); }

            @Override
            protected Paragraph<S> computeValue() {
                int i = Math.min(caretRow.get(), content.paragraphs.size()-1); // in case lines were removed before updating caretRow
                return content.paragraphs.get(i);
            }
        };

        // Keep caret position in the current line up to date.
        InvalidationListener updateCaretPosInCurrentLine = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                int pos = Math.min(caretCol.get(), currentLine.get().length()); // because caretCol and currentLine are not updated atomically
                currentLine.get().setCaretPosition(pos);
            }
        };
        caretCol.addListener(updateCaretPosInCurrentLine);
        currentLine.addListener(updateCaretPosInCurrentLine);

        selectedText.bind(new StringBinding() {
            { bind(selection, content); }
            @Override protected String computeValue() {
                IndexRange sel = selection.get();
                int start = sel.getStart();
                int end = sel.getEnd();

                int textLength = content.length().get();
                end = Math.min(end, textLength);
                if(start >= textLength)
                    start = end = 0;

                return content.get(start, end);
            }
        });

        getStyleClass().add("styled-text-area");
    }


    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    @Override
    public Position position(int row, int col) {
        return content.position(row, col);
    }

    @Override
    public Position offsetToPosition(int charOffset) {
        return content.offsetToPosition(charOffset);
    }

    @Override
    public String getText(int paragraph) {
        return content.paragraphs.get(paragraph).toString();
    }

    /**
     * Returns an unmodifiable list of paragraphs
     * that back this code area's content.
     */
    @Override
    public ObservableList<Paragraph<S>> getParagraphs() {
        return FXCollections.unmodifiableObservableList(content.paragraphs);
    }

    /**
     * Sets style for the given character range.
     */
    public void setStyle(int from, int to, S style) {
        content.setStyle(from, to, style);
    }

    /**
     * Sets style for the whole paragraph.
     */
    public void setStyle(int paragraph, S style) {
        content.setStyle(paragraph, style);
    }

    /**
     * Sets style for the given range relative in the given paragraph.
     */
    public void setStyle(int paragraph, int from, int to, S style) {
        content.setStyle(paragraph, from, to, style);
    }

    /**
     * Resets the style of the given range to the initial style.
     */
    public void clearStyle(int from, int to) {
        setStyle(from, to, initialStyle);
    }

    /**
     * Resets the style of the given paragraph to the initial style.
     */
    public void clearStyle(int paragraph) {
        setStyle(paragraph, initialStyle);
    }

    /**
     * Resets the style of the given range in the given paragraph
     * to the initial style.
     */
    public void clearStyle(int paragraph, int from, int to) {
        setStyle(paragraph, from, to, initialStyle);
    }

    /**
     * Returns the style of the character at the given position.
     */
    public S getStyleAt(int pos) {
        return content.getStyleAt(pos);
    }

    /**
     * Returns the style of the character at the given position in the given
     * paragraph. If {@code pos} is beyond the end of the paragraph, the style
     * at the end of line is returned. If {@code pos} is negative, it is the
     * same as if it was 0.
     */
    public S getStyleAt(int paragraph, int pos) {
        return content.getStyleAt(paragraph, pos);
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new StyledTextAreaSkin<S>(this, applyStyle);
    }

    @Override
    public void replaceText(int start, int end, String text) {
        start = Utils.clamp(0, start, getLength());
        end = Utils.clamp(0, end, getLength());

        content.replaceText(start, end, text);

        int newCaretPos = start + text.length();
        selectRange(newCaretPos, newCaretPos);
    }

    @Override
    public void selectRange(int anchor, int caretPosition) {
        this.caretPosition.set(Utils.clamp(0, caretPosition, getLength()));
        this.anchor.set(Utils.clamp(0, anchor, getLength()));
        this.selection.set(IndexRange.normalize(getAnchor(), this.caretPosition.get()));
    }

    @Override
    public void positionCaret(int pos) {
        caretPosition.set(pos);
    }


    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return Arrays.<CssMetaData<? extends Styleable, ?>>asList(
                font.getCssMetaData());
    }
}
