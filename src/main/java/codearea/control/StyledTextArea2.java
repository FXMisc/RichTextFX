package codearea.control;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableIntegerValue;
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
import codearea.control.CssProperties.EditableProperty;
import codearea.control.CssProperties.FontProperty;
import codearea.rx.Source;
import codearea.skin.StyledTextAreaSkin;
import codearea.undo.UndoManager;
import codearea.undo.UndoManagerProvider;
import codearea.undo.impl.ObservingUndoManager;

import com.sun.javafx.Utils;

/**
 * Text editing control. Accepts user input (keyboard, mouse) and
 * provides API to assign style to text ranges. It is suitable for
 * syntax highlighting and rich-text editors.
 *
 * <p>Subclassing is allowed to define the type of style, e.g. inline
 * style or style classes.</p>
 *
 * @param <S> type of style that can be applied to text.
 */
public class StyledTextArea2<S> extends StyledTextAreaBase<S> {

    private static final UndoManagerProvider<TextChange> defaultUndoManagerFactory =
            (apply, undo, merge, changeSource) -> new ObservingUndoManager<>(apply, undo, merge, changeSource);


    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     * Properties affect behavior and/or appearance of this control.           *
     *                                                                         *
     * They are readable and writable by the client code and never change by   *
     * other means, i.e. they contain either the default value or the value    *
     * set by the client code.                                                 *
     *                                                                         *
     **************************************************************************/

    // editable property
    private final BooleanProperty editable = new EditableProperty<>(this);
    @Override public final boolean isEditable() { return editable.get(); }
    @Override public final void setEditable(boolean value) { editable.set(value); }
    @Override public final BooleanProperty editableProperty() { return editable; }

    // wrapText property
    private final BooleanProperty wrapText = new SimpleBooleanProperty(this, "wrapText");
    @Override public final boolean isWrapText() { return wrapText.get(); }
    @Override public final void setWrapText(boolean value) { wrapText.set(value); }
    @Override public final BooleanProperty wrapTextProperty() { return wrapText; }

    // undo manager
    private UndoManager undoManager;
    @Override
    public UndoManager getUndoManager() { return undoManager; }
    @Override
    public void setUndoManager(UndoManagerProvider<TextChange> undoManagerProvider) {
        undoManager.close();
        undoManager = createUndoManager(undoManagerProvider);
    }

    // font property
    /**
     * The default font to use where font is not specified otherwise.
     */
    private final StyleableObjectProperty<Font> font = new FontProperty<>(this);
    public final StyleableObjectProperty<Font> fontProperty() { return font; }
    public final void setFont(Font value) { font.setValue(value); }
    public final Font getFont() { return font.getValue(); }


    /***************************************************************************
     *                                                                         *
     * Observables                                                             *
     *                                                                         *
     * Observables are "dynamic" (i.e. changing) characteristics of this       *
     * control. They are not directly settable by the client code, but change  *
     * in response to user input and/or API actions.                           *
     *                                                                         *
     **************************************************************************/

    // text
    @Override public final String getText() { return content.get(); }
    @Override public final ObservableStringValue textProperty() { return content; }

    // length
    @Override public final int getLength() { return content.length().get(); }
    @Override public final ObservableIntegerValue lengthProperty() { return content.length(); }

    // caret position
    private final ReadOnlyIntegerWrapper caretPosition = new ReadOnlyIntegerWrapper(this, "caretPosition", 0);
    @Override public final int getCaretPosition() { return caretPosition.get(); }
    @Override public final ReadOnlyIntegerProperty caretPositionProperty() { return caretPosition.getReadOnlyProperty(); }

    // selection anchor
    private final ReadOnlyIntegerWrapper anchor = new ReadOnlyIntegerWrapper(this, "anchor", 0);
    @Override public final int getAnchor() { return anchor.get(); }
    @Override public final ReadOnlyIntegerProperty anchorProperty() { return anchor.getReadOnlyProperty(); }

    // selection
    private final ReadOnlyObjectWrapper<IndexRange> selection = new ReadOnlyObjectWrapper<IndexRange>(this, "selection", new IndexRange(0, 0));
    @Override public final IndexRange getSelection() { return selection.getValue(); }
    @Override public final ReadOnlyObjectProperty<IndexRange> selectionProperty() { return selection.getReadOnlyProperty(); }

    // selected text
    private final ObservableStringValue selectedText;
    @Override public final String getSelectedText() { return selectedText.get(); }
    @Override public final ObservableStringValue selectedTextProperty() { return selectedText; }

    // current paragraph index
    private final ObservableIntegerValue currentParagraph;
    @Override public final int getCurrentParagraph() { return currentParagraph.get(); }
    @Override public final ObservableIntegerValue currentParagraph() { return currentParagraph; }

    // caret column
    private final ObservableIntegerValue caretColumn;
    @Override public final int getCaretColumn() { return caretColumn.get(); }

    // paragraphs
    @Override public ObservableList<Paragraph<S>> getParagraphs() {
        return FXCollections.unmodifiableObservableList(content.paragraphs);
    }


    /***************************************************************************
     *                                                                         *
     * Event streams                                                           *
     *                                                                         *
     **************************************************************************/

    // text changes
    @Override
    public final Source<TextChange> textChanges() { return content.textChanges(); }


    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     ***************************************************************************/

    /**
     * content model
     */
    private final StyledTextAreaContent<S> content;

    /**
     * Style used by default when no other style is provided.
     */
    private final S initialStyle;

    /**
     * Style applicator used by the default skin.
     */
    private final BiConsumer<Text, S> applyStyle;


    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     ***************************************************************************/

    /**
     * Creates a text area with empty text content.
     *
     * @param initialStyle style to use in places where no other style is
     * specified (yet).
     * @param applyStyle function that, given a {@link Text} node and
     * a style, applies the style to the text node. This function is
     * used by the default skin to apply style to text nodes.
     */
    public StyledTextArea2(S initialStyle, BiConsumer<Text, S> applyStyle) {
        this.initialStyle = initialStyle;
        this.applyStyle = applyStyle;
        content = new StyledTextAreaContent<>(initialStyle);

        undoManager = createUndoManager(defaultUndoManagerFactory);

        ObservableValue<Position> caretPosition2D = createBinding(caretPosition, p -> content.offsetToPosition(p));
        currentParagraph = createIntegerBinding(caretPosition2D, p -> p.getMajor());
        caretColumn = createIntegerBinding(caretPosition2D, p -> p.getMinor());

        // Keep caret position in the current paragraph up to date.
        caretPosition.addListener(obs -> {
            // by the time this listener is called, both currentParagraph and
            // caretColumn have been invalidated, so get()-ing them yields
            // up-to-date values.
            Paragraph<S> par = content.paragraphs.get(currentParagraph.get());
            par.setCaretPosition(caretColumn.get());
        });

        selectedText = new StringBinding() {
            { bind(selection, textProperty()); }
            @Override protected String computeValue() {
                return content.get(selection.get());
            }
        };

        getStyleClass().add("styled-text-area");
    }


    /***************************************************************************
     *                                                                         *
     * Queries                                                                 *
     *                                                                         *
     * Queries are parameterized observables.                                  *
     *                                                                         *
     ***************************************************************************/

    @Override
    public final String getText(int start, int end) {
        return content.get(start, end);
    }

    @Override
    public String getText(int paragraph) {
        return content.paragraphs.get(paragraph).toString();
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

    @Override
    public Position position(int row, int col) {
        return content.position(row, col);
    }

    @Override
    public Position offsetToPosition(int charOffset) {
        return content.offsetToPosition(charOffset);
    }


    /***************************************************************************
     *                                                                         *
     * Actions                                                                 *
     *                                                                         *
     * Actions change the state of this control. They typically cause a change *
     * of one or more observables and/or produce an event.                     *
     *                                                                         *
     **************************************************************************/

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
        IndexRange selection = IndexRange.normalize(getAnchor(), getCaretPosition());

        // update selection in paragraphs
        int start = selection.getStart();
        int end = selection.getEnd();
        for(Paragraph<S> par: content.paragraphs) {
            int len = par.length();
            if(end > 0 && start < len) {
                par.setSelection(start, Math.min(end, len));
            } else {
                par.setSelection(0, 0);
            }
            start = start - (len+1);
            end = end - (len+1);
        }

        this.selection.set(selection);
    }

    @Override
    public void positionCaret(int pos) {
        caretPosition.set(pos);
    }


    /***************************************************************************
     *                                                                         *
     * Look & feel                                                             *
     *                                                                         *
     **************************************************************************/

    @Override
    protected Skin<?> createDefaultSkin() {
        return new StyledTextAreaSkin<S>(this, applyStyle);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return Arrays.<CssMetaData<? extends Styleable, ?>>asList(
                font.getCssMetaData());
    }


    /***************************************************************************
     *                                                                         *
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/

    private UndoManager createUndoManager(UndoManagerProvider<TextChange> factory) {
        Consumer<TextChange> apply = change -> replaceText(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted());
        Consumer<TextChange> undo = change -> replaceText(change.getPosition(), change.getPosition() + change.getInserted().length(), change.getRemoved());
        BiFunction<TextChange, TextChange, Optional<TextChange>> merge = (change1, change2) -> change1.mergeWith(change2);
        return factory.get(apply, undo, merge, textChanges());
    }

    private static <T> ObjectBinding<T> createBinding(ObservableIntegerValue dep, IntFunction<T> computeValue) {
        return new ObjectBinding<T>() {
            { bind(dep); }

            @Override
            protected T computeValue() {
                return computeValue.apply(dep.get());
            }
        };
    }

    private static <A> IntegerBinding createIntegerBinding(ObservableValue<A> dep, ToIntFunction<A> computeValue) {
        return new IntegerBinding() {
            { bind(dep); }

            @Override
            protected int computeValue() {
                return computeValue.applyAsInt(dep.getValue());
            }
        };
    }
}
