package org.fxmisc.richtext.actions;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.MouseOverTextEvent;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.actions.NavigationActions.SelectionPolicy;
import org.reactfx.util.Tuple2;
import org.reactfx.value.Var;

import java.time.Duration;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public interface ViewActions<PS, SEG, S> {

    /**
     * Indicates whether this text area can be edited by the user.
     * Note that this property doesn't affect editing through the API.
     */
    BooleanProperty editableProperty();
    boolean isEditable();
    void setEditable(boolean value);

    /**
     * When a run of text exceeds the width of the text region,
     * then this property indicates whether the text should wrap
     * onto another line.
     */
    BooleanProperty wrapTextProperty();
    boolean isWrapText();
    void setWrapText(boolean value);

    public static enum CaretVisibility {
        /** Caret is displayed. */
        ON,
        /** Caret is displayed when area is focused, enabled, and editable. */
        AUTO,
        /** Caret is not displayed. */
        OFF
    }

    /**
     * Indicates when this text area should display a caret.
     */
    Var<CaretVisibility> showCaretProperty();
    CaretVisibility getShowCaret();
    void setShowCaret(CaretVisibility value);

    /**
     * Defines how long the mouse has to stay still over the text before a
     * {@link MouseOverTextEvent} of type {@code MOUSE_OVER_TEXT_BEGIN} is
     * fired on this text area. When set to {@code null}, no
     * {@code MouseOverTextEvent}s are fired on this text area.
     *
     * <p>Default value is {@code null}.
     */
    ObjectProperty<Duration> mouseOverTextDelayProperty();
    void setMouseOverTextDelay(Duration delay);
    Duration getMouseOverTextDelay();

    /**
     * Defines how to handle an event in which the user has selected some text, dragged it to a
     * new location within the area, and released the mouse at some character {@code index}
     * within the area.
     *
     * <p>By default, this will relocate the selected text to the character index where the mouse
     * was released. To override it, use {@link #setOnSelectionDrop(IntConsumer)}.
     */
    ObjectProperty<IntConsumer> onSelectionDropProperty();
    void setOnSelectionDrop(IntConsumer consumer);
    IntConsumer getOnSelectionDrop();

    ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactoryProperty();
    void setParagraphGraphicFactory(IntFunction<? extends Node> factory);
    IntFunction<? extends Node> getParagraphGraphicFactory();

    /**
     * Sets codecs to encode/decode style information to/from binary format.
     * Providing codecs enables clipboard actions to retain the style information.
     */
    void setStyleCodecs(Codec<PS> paragraphStyleCodec, Codec<SEG> textStyleCodec);
    Optional<Tuple2<Codec<PS>, Codec<SEG>>> getStyleCodecs();

    double getEstimatedScrollX();
    void setEstimatedScrollX(double value);

    double getEstimatedScrollY();
    void setEstimatedScrollY(double value);

    double getTotalWidthEstimate();
    double getTotalHeightEstimate();

    /**
     * Gets the height of the underlying {@link org.fxmisc.flowless.VirtualFlow}.
     */
    double getViewportHeight();

    /**
     * Gets the width of the underlying {@link org.fxmisc.flowless.VirtualFlow}.
     */
    double getViewportWidth();

    /**
     * Helpful for determining which letter is at point x, y:
     * <pre>
     *     {@code
     *     StyledTextArea area = // creation code
     *     area.addEventHandler(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
     *         CharacterHit hit = area.hit(e.getX(), e.getY());
     *         int characterPosition = hit.getInsertionIndex();
     *
     *         // move the caret to that character's position
     *         area.moveTo(characterPosition, SelectionPolicy.CLEAR);
     *     }}
     * </pre>
     */
    CharacterHit hit(double x, double y);

    /**
     * Gets the number of lines a paragraph spans when {@link #isWrapText()} is true, or otherwise returns 1.
     */
    public int getParagraphLinesCount(int paragraphIndex);

    /**
     * Scroll area horizontally by {@code deltas.getX()} and vertically by {@code deltas.getY()}
     * @param deltas negative values scroll left/up, positive scroll right/down
     */
    void scrollBy(Point2D deltas);

    /**
     * Shows the paragraph somewhere in the viewport. If the line is already visible, no noticeable change occurs.
     * If line is above the current view, it appears at the top of the viewport. If the line is below the current
     * view, it appears at the bottom of the viewport.
     */
    void showParagraphInViewport(int paragraphIndex);

    /**
     * Lays out the viewport so that the paragraph is the first line (top) displayed in the viewport. Note: if
     * the given area does not have enough lines that follow the given line to span its entire height, the paragraph
     * may not appear at the very top of the viewport. Instead, it may simply be shown in the viewport. For example,
     * given an unwrapped area whose height could show 10 lines but whose content only has 3 lines, calling
     * {@code showParagraphAtTop(3)} would be no different than {@code showParagraphAtTop(1)}.
     */
    void showParagraphAtTop(int paragraphIndex);

    /**
     * Lays out the viewport so that the paragraph is the last line (bottom) displayed in the viewport. Note: if
     * the given area does not have enough lines preceding the given line to span its entire height, the paragraph
     * may not appear at the very bottom of the viewport. Instead, it may appear towards the bottom of the viewport
     * with some extra space following it. For example, given an unwrapped area whose height could show 10 lines but
     * whose content only has 7 lines, calling {@code showParagraphAtBottom(1)} would be no different than calling
     * {@code showParagraphAtBottom(7)}.
     */
    void showParagraphAtBottom(int paragraphIndex);

    /**
     * If the caret is not visible within the area's view, the area will scroll so that caret
     * is visible in the next layout pass. Use this method when you wish to "follow the caret"
     * (i.e. auto-scroll to caret) after making a change (add/remove/modify area's segments).
     */
    void requestFollowCaret();

    /**
     * Move the caret to the start of either the line in a multi-line wrapped paragraph or the paragraph
     * in a single-line / non-wrapped paragraph
     *
     * @param policy use {@link SelectionPolicy#CLEAR} when no selection is desired and
     *                        {@link SelectionPolicy#ADJUST} when a selection from starting point
     *                        to the place to where the caret is moved is desired.
     */
    void lineStart(SelectionPolicy policy);

    /**
     * Move the caret to the end of either the line in a multi-line wrapped paragraph or the paragraph
     * in a single-line / non-wrapped paragraph
     *
     * @param policy use {@link SelectionPolicy#CLEAR} when no selection is desired and
     *                        {@link SelectionPolicy#ADJUST} when a selection from starting point
     *                        to the place to where the caret is moved is desired.
     */
    void lineEnd(SelectionPolicy policy);

    /**
     * Selects the current line.
     */
    void selectLine();

    /**
     * Moves caret to the previous page (i.e. page up)
     * @param selectionPolicy use {@link SelectionPolicy#CLEAR} when no selection is desired and
     *                        {@link SelectionPolicy#ADJUST} when a selection from starting point
     *                        to the place to where the caret is moved is desired.
     */
    void prevPage(SelectionPolicy selectionPolicy);

    /**
     * Moves caret to the next page (i.e. page down)
     * @param selectionPolicy use {@link SelectionPolicy#CLEAR} when no selection is desired and
     *                        {@link SelectionPolicy#ADJUST} when a selection from starting point
     *                        to the place to where the caret is moved is desired.
     */
    void nextPage(SelectionPolicy selectionPolicy);
}
