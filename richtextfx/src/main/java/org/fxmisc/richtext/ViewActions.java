package org.fxmisc.richtext;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.text.TextFlow;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public interface ViewActions<PS, SEG, S> {

    /**
     * Indicates whether this text area can be edited by the user.
     * Note that this property doesn't affect editing through the API.
     */
    boolean isEditable();
    void setEditable(boolean value);
    BooleanProperty editableProperty();

    /**
     * When a run of text exceeds the width of the text region,
     * then this property indicates whether the text should wrap
     * onto another line.
     */
    boolean isWrapText();
    void setWrapText(boolean value);
    BooleanProperty wrapTextProperty();

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
    CaretVisibility getShowCaret();
    void setShowCaret(CaretVisibility value);
    Var<CaretVisibility> showCaretProperty();

    /**
     * Defines how long the mouse has to stay still over the text before a
     * {@link MouseOverTextEvent} of type {@code MOUSE_OVER_TEXT_BEGIN} is
     * fired on this text area. When set to {@code null}, no
     * {@code MouseOverTextEvent}s are fired on this text area.
     *
     * <p>Default value is {@code null}.
     */
    Duration getMouseOverTextDelay();
    void setMouseOverTextDelay(Duration delay);
    ObjectProperty<Duration> mouseOverTextDelayProperty();

    /**
     * Defines how to handle an event in which the user has selected some text, dragged it to a
     * new location within the area, and released the mouse at some character {@code index}
     * within the area.
     *
     * <p>By default, this will relocate the selected text to the character index where the mouse
     * was released. To override it, use {@link #setOnSelectionDrop(IntConsumer)}.
     */
    IntConsumer getOnSelectionDrop();
    void setOnSelectionDrop(IntConsumer consumer);

    /**
     * Gets the function that maps a line index to a node that is positioned to the left of the first character
     * in a paragraph's text. Useful for toggle points or indicating the line's number via {@link LineNumberFactory}.
     */
    IntFunction<? extends Node> getParagraphGraphicFactory();
    void setParagraphGraphicFactory(IntFunction<? extends Node> factory);
    ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactoryProperty();

    /** Gets the {@link ContextMenu} for the area, which is by default null. */
    ContextMenu getContextMenu();
    void setContextMenu(ContextMenu menu);
    ObjectProperty<ContextMenu> contextMenuObjectProperty();

    /**
     * Gets the horizontal amount in pixels by which to offset the {@link #getContextMenu()} when it is shown,
     * which has a default value of 2.
     */
    double getContextMenuXOffset();
    void setContextMenuXOffset(double offset);

    /**
     * Gets the vertical amount in pixels by which to offset the {@link #getContextMenu()} when it is shown,
     * which has a default value of 2.
     */
    double getContextMenuYOffset();
    void setContextMenuYOffset(double offset);

    /**
     * Gets the bounds of the caret in the Screen's coordinate system or {@link Optional#empty()}
     * if caret is not visible in the viewport.
     */
    Optional<Bounds> getCaretBounds();
    ObservableValue<Optional<Bounds>> caretBoundsProperty();

    /**
     * Gets the bounds of the selection in the Screen's coordinate system if something is selected and visible in the
     * viewport or {@link Optional#empty()} if selection is not visible in the viewport.
     */
    Optional<Bounds> getSelectionBounds();
    ObservableValue<Optional<Bounds>> selectionBoundsProperty();

    /**
     * Gets the <em>estimated</em> scrollX value. This can be set in order to scroll the content.
     * Value is only accurate when area does not wrap lines and uses the same font size
     * throughout the entire area.
     */
    double getEstimatedScrollX();
    void setEstimatedScrollX(double value);
    Var<Double> estimatedScrollXProperty();

    /**
     * Gets the <em>estimated</em> scrollY value. This can be set in order to scroll the content.
     * Value is only accurate when area does not wrap lines and uses the same font size
     * throughout the entire area.
     */
    double getEstimatedScrollY();
    void setEstimatedScrollY(double value);
    Var<Double> estimatedScrollYProperty();

    /**
     * Gets the <em>estimated</em> width of the entire document. Accurate when area does not wrap lines and
     * uses the same font size throughout the entire area. Value is only supposed to be <em>set</em> by
     * the skin, not the user.
     */
    double getTotalWidthEstimate();
    Val<Double> totalWidthEstimateProperty();

    /**
     * Gets the <em>estimated</em> height of the entire document. Accurate when area does not wrap lines and
     * uses the same font size throughout the entire area. Value is only supposed to be <em>set</em> by
     * the skin, not the user.
     */
    double getTotalHeightEstimate();
    Val<Double> totalHeightEstimateProperty();

    /**
     * Gets the style applicator.
     */
    BiConsumer<TextFlow, PS> getApplyParagraphStyle();

    /* ********************************************************************** *
     *                                                                        *
     * Queries                                                                *
     *                                                                        *
     * Queries are parameterized observables.                                 *
     *                                                                        *
     * ********************************************************************** */

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
    int getParagraphLinesCount(int paragraphIndex);

    /**
     * Gets the character bounds on screen
     *
     * @param from the start position
     * @param to the end position
     * @return the bounds or {@link Optional#empty()} if line is not visible or the range is only a newline character.
     */
    Optional<Bounds> getCharacterBoundsOnScreen(int from, int to);

    /* ********************************************************************** *
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * Actions change the state of this control. They typically cause a       *
     * change of one or more observables and/or produce an event.             *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Scroll area horizontally by {@code deltas.getX()} and vertically by {@code deltas.getY()}
     *
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
     * @param policy use {@link NavigationActions.SelectionPolicy#CLEAR} when no selection is desired an
     *               {@link NavigationActions.SelectionPolicy#ADJUST} when a selection from starting point
     *               to the place to where the caret is moved is desired.
     */
    void lineStart(NavigationActions.SelectionPolicy policy);

    /**
     * Move the caret to the end of either the line in a multi-line wrapped paragraph or the paragraph
     * in a single-line / non-wrapped paragraph
     *
     * @param policy use {@link NavigationActions.SelectionPolicy#CLEAR} when no selection is desired an
     *               {@link NavigationActions.SelectionPolicy#ADJUST} when a selection from starting point
     *               to the place to where the caret is moved is desired.
     */
    void lineEnd(NavigationActions.SelectionPolicy policy);

    /**
     * Selects the current line of a multi-line paragraph.
     */
    void selectLine();

    /**
     * Moves caret to the previous page (i.e. page up)
     *
     * @param selectionPolicy use {@link NavigationActions.SelectionPolicy#CLEAR} when no selection is desired and
     *                        {@link NavigationActions.SelectionPolicy#ADJUST} when a selection from starting point
     *                        to the place to where the caret is moved is desired.
     */
    void prevPage(NavigationActions.SelectionPolicy selectionPolicy);

    /**
     * Moves caret to the next page (i.e. page down)
     *
     * @param selectionPolicy use {@link NavigationActions.SelectionPolicy#CLEAR} when no selection is desired and
     *                        {@link NavigationActions.SelectionPolicy#ADJUST} when a selection from starting point
     *                        to the place to where the caret is moved is desired.
     */
    void nextPage(NavigationActions.SelectionPolicy selectionPolicy);

}
