package org.fxmisc.richtext;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextFlow;

import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.reactfx.EventStream;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Specifies view-related API for a {@link TextEditingArea}
 *
 * @param <PS> paragraph style type
 * @param <SEG> segment type
 * @param <S> segment style type
 */
public interface ViewActions<PS, SEG, S> {

    /**
     * Indicates whether this text area can be edited by the user.
     * Note that this property doesn't affect editing through the API.
     */
    default boolean isEditable() { return editableProperty().get(); }
    default void setEditable(boolean value) { editableProperty().set(value); }
    BooleanProperty editableProperty();

    /**
     * When a run of text exceeds the width of the text region,
     * then this property indicates whether the text should wrap
     * onto another line.
     */
    default boolean isWrapText() { return wrapTextProperty().get(); }
    default void setWrapText(boolean value) { wrapTextProperty().set(value); }
    BooleanProperty wrapTextProperty();

    /**
     * Defines how long the mouse has to stay still over the text before a
     * {@link MouseOverTextEvent} of type {@code MOUSE_OVER_TEXT_BEGIN} is
     * fired on this text area. When set to {@code null}, no
     * {@code MouseOverTextEvent}s are fired on this text area.
     *
     * <p>Default value is {@code null}.
     */
    default Duration getMouseOverTextDelay() { return mouseOverTextDelayProperty().get(); }
    default void setMouseOverTextDelay(Duration delay) { mouseOverTextDelayProperty().set(delay); }
    ObjectProperty<Duration> mouseOverTextDelayProperty();

    /**
     * Indicates whether area should auto scroll towards a {@link MouseEvent#MOUSE_DRAGGED} event. This can be
     * used when additional drag behavior is added on top of the area's default drag behavior and one does not
     * want this auto scroll feature to occur. This flag should be set to the correct value before the end of
     * the process InputMap.
     */
    default boolean isAutoScrollOnDragDesired() { return autoScrollOnDragDesiredProperty().get(); }
    default void setAutoScrollOnDragDesired(boolean val) { autoScrollOnDragDesiredProperty().set(val); }
    BooleanProperty autoScrollOnDragDesiredProperty();

    /**
     * Runs the EventHandler when the user pressed the mouse over unselected text within the area.
     *
     * <p>By default, this will {@link NavigationActions#moveTo(int) move the caret} to the position where
     * the mouse was pressed and clear out any selection via the code:
     * <pre><code>
     *     e -&gt; {
     *         CharacterHit hit = hit(e.getX(), e.getY());
     *         moveTo(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
     *     }
     * </code></pre>.
     */
    void setOnOutsideSelectionMousePressed(EventHandler<MouseEvent> handler);
    ObjectProperty<EventHandler<MouseEvent>> onOutsideSelectionMousePressedProperty();
    EventHandler<MouseEvent> getOnOutsideSelectionMousePressed();

    /**
     * Runs the EventHandler when the mouse is released in this scenario: the user has selected some text and then
     * "clicked" the mouse somewhere in that selection (the use pressed the mouse, did not drag it,
     * and released the mouse). <em>Note:</em> this consumer is run on {@link MouseEvent#MOUSE_RELEASED},
     * not {@link MouseEvent#MOUSE_CLICKED}.
     *
     * <p>By default, this will {@link NavigationActions#moveTo(int) move the caret} to the position where
     * the mouse was clicked and clear out any selection via the code:
     * <pre><code>
     *     e -&gt; {
     *         CharacterHit hit = hit(e.getX(), e.getY());
     *         moveTo(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
     *     }
     * </code></pre>.
     */
    void setOnInsideSelectionMousePressReleased(EventHandler<MouseEvent> handler);
    ObjectProperty<EventHandler<MouseEvent>> onInsideSelectionMousePressReleasedProperty();
    EventHandler<MouseEvent> getOnInsideSelectionMousePressReleased();

    /**
     * Runs the consumer when the mouse is dragged in this scenario: the user has pressed the mouse over some
     * unselected text, and dragged the mouse to a new location within the area, but has not yet released the mouse.
     * Each time the user drags the mouse without releasing it, this hook's consumer gets called.
     *
     * <p>By default, this will create a new selection or
     * {@link NavigationActions.SelectionPolicy#ADJUST} the current one to be bigger or
     * smaller via the code:
     * <pre><code>
     *     e -&gt; {
     *         CharacterHit hit = hit(e.getX(), e.getY());
     *         moveTo(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
     *     }
     * </code></pre>.
     */
    default Consumer<Point2D> getOnNewSelectionDrag() { return onNewSelectionDragProperty().get(); }
    default void setOnNewSelectionDrag(Consumer<Point2D> consumer) { onNewSelectionDragProperty().set(consumer); }
    ObjectProperty<Consumer<Point2D>> onNewSelectionDragProperty();

    /**
     * Runs the EventHandler when the mouse is released in this scenario: the user has pressed the mouse over some
     * unselected text, and dragged the mouse to a new location within the area, and released the mouse.
     *
     * <p>By default, this will {@link NavigationActions.SelectionPolicy#ADJUST} the
     * current selection to be bigger or smaller via the code:
     * <pre><code>
     *     e -&gt; {
     *         CharacterHit hit = hit(e.getX(), e.getY());
     *         moveTo(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
     *     }
     * </code></pre>.
     */
    void setOnNewSelectionDragFinished(EventHandler<MouseEvent> handler);
    ObjectProperty<EventHandler<MouseEvent>> onNewSelectionDragFinishedProperty();
    EventHandler<MouseEvent> getOnNewSelectionDragFinished();

    /**
     * Runs the consumer when the mouse is dragged in this scenario: the user has selected some text,
     * pressed the mouse on top of the selection, dragged it to a new location within the area,
     * but has not yet released the mouse. Each time the user drags the mouse without releasing it,
     * this hook's consumer gets called.
     *
     * <p>By default, this will {@link GenericStyledArea#displaceCaret(int) displace the caret} to that position
     * within the area via the code:
     * <pre><code>
     *     p -&gt; {
     *         CharacterHit hit = hit(p.getX(), p.getY());
     *         displaceCaret(hit.getInsertionIndex());
     *     }
     * </code></pre>.
     */
    default Consumer<Point2D> getOnSelectionDrag() { return onSelectionDragProperty().get(); }
    default void setOnSelectionDrag(Consumer<Point2D> consumer) { onSelectionDragProperty().set(consumer); }
    ObjectProperty<Consumer<Point2D>> onSelectionDragProperty();

    /**
     * Runs the EventHandler when the mouse is released in this scenario: the user has selected some text,
     * pressed the mouse on top of the selection, dragged it to a new location within the area,
     * and released the mouse within the area.
     *
     * <p>By default, this will relocate the selected text to the character index where the mouse
     * was released via the code:
     * <pre><code>
     *     e -&gt; {
     *         CharacterHit hit = hit(e.getX(), e.getY());
     *         moveSelectedText(hit.getInsertionIndex());
     *     }
     * </code></pre>.
     */
    void setOnSelectionDropped(EventHandler<MouseEvent> handler);
    ObjectProperty<EventHandler<MouseEvent>> onSelectionDroppedProperty();
    EventHandler<MouseEvent> getOnSelectionDropped();

    /**
     * Gets the function that maps a line index to a node that is positioned to the left of the first character
     * in a paragraph's text. Useful for toggle points or indicating the line's number via {@link LineNumberFactory}.
     */
    default IntFunction<? extends Node> getParagraphGraphicFactory() { return paragraphGraphicFactoryProperty().get(); }
    default void setParagraphGraphicFactory(IntFunction<? extends Node> factory) { paragraphGraphicFactoryProperty().set(factory); }
    ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactoryProperty();

    /** Gets the {@link ContextMenu} for the area, which is by default null. */
    default ContextMenu getContextMenu() { return contextMenuObjectProperty().get(); }
    default void setContextMenu(ContextMenu menu) { contextMenuObjectProperty().set(menu); }
    ObjectProperty<ContextMenu> contextMenuObjectProperty();

    /**
     * Gets the horizontal amount in pixels by which to offset the {@link #getContextMenu()} when it is shown,
     * which has a default value of 2.
     */
    default double getContextMenuXOffset() { return contextMenuXOffsetProperty().get(); }
    default void setContextMenuXOffset(double offset) { contextMenuXOffsetProperty().set(offset); }
    DoubleProperty contextMenuXOffsetProperty();

    /**
     * Gets the vertical amount in pixels by which to offset the {@link #getContextMenu()} when it is shown,
     * which has a default value of 2.
     */
    default double getContextMenuYOffset() { return contextMenuYOffsetProperty().get(); }
    default void setContextMenuYOffset(double offset) { contextMenuYOffsetProperty().set(offset); }
    DoubleProperty contextMenuYOffsetProperty();

    /**
     * The <em>estimated</em> scrollX value. This can be set in order to scroll the content.
     * Value is only accurate when area does not wrap lines and uses the same font size
     * throughout the entire area.
     */
    Var<Double> estimatedScrollXProperty();

    /**
     * The <em>estimated</em> scrollY value. This can be set in order to scroll the content.
     * Value is only accurate when area does not wrap lines and uses the same font size
     * throughout the entire area.
     */
    Var<Double> estimatedScrollYProperty();

    /**
     * The <em>estimated</em> width of the entire document. Accurate when area does not wrap lines and
     * uses the same font size throughout the entire area. Value is only supposed to be <em>set</em> by
     * the skin, not the user.
     */
    Val<Double> totalWidthEstimateProperty();

    /**
     * The <em>estimated</em> height of the entire document. Accurate when area does not wrap lines and
     * uses the same font size throughout the entire area. Value is only supposed to be <em>set</em> by
     * the skin, not the user.
     */
    Val<Double> totalHeightEstimateProperty();

    /**
     * Returns an {@link EventStream} that emits a {@code null} value every time the viewport becomes dirty (e.g.
     * the viewport's width, height, scaleX, scaleY, estimatedScrollX, or estimatedScrollY values change)
     */
    public EventStream<?> viewportDirtyEvents();

    /**
     * Gets the visible paragraphs, even the ones that are barely displayed.
     */
    LiveList<Paragraph<PS, SEG, S>> getVisibleParagraphs();

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
     * Gets the height of the viewport and ignores the padding values added to the area.
     */
    public double getViewportHeight();

    /**
     * Maps a paragraph index from {@link TextEditingArea#getParagraphs()} into the index system of
     * {@link #getVisibleParagraphs()}.
     */
    public Optional<Integer> allParToVisibleParIndex(int allParIndex);

    /**
     * Maps a paragraph index from {@link #getVisibleParagraphs()} into the index system of
     * {@link TextEditingArea#getParagraphs()}.
     */
    public int visibleParToAllParIndex(int visibleParIndex);

    /**
     * Returns the index of the first visible paragraph in the index system of {@link TextEditingArea#getParagraphs()}.
     */
    default int firstVisibleParToAllParIndex() { return visibleParToAllParIndex(0); }

    /**
     * Returns the index of the last visible paragraph in the index system of {@link TextEditingArea#getParagraphs()}.
     */
    default int lastVisibleParToAllParIndex() { return visibleParToAllParIndex(getVisibleParagraphs().size() - 1); }

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
     * Returns 0 if the given paragraph displays its content across only one line, or returns the index
     * of the line on which the given column position appears if the paragraph spans multiple lines.
     */
    public int lineIndex(int paragraphIndex, int columnPosition);

    /**
     * Gets the number of lines a paragraph spans when {@link #isWrapText()} is true, or otherwise returns 1.
     * CAUTION: the underlying TextFlow does not immediately account for changes in the stage's width when the
     * paragraph in question is a multi-line paragraph and {@link #isWrapText() text wrap is on}. After calling
     * {@link javafx.stage.Stage#setWidth(double)}, it may take anywhere between 150-300 milliseconds for TextFlow
     * to account for this and return the correct line count for the given paragraph. Otherwise, it may return a
     * skewed number, such as the total number of characters on the line.
     */
    int getParagraphLinesCount(int paragraphIndex);

    /**
     * Using the paragraph index of the "all paragraph" index system, returns the bounds of a caret on the
     * given paragraph or {@link Optional#empty()} if no caret is on that paragraph or the pragraph is not visible.
     */
    public <T extends Node & Caret> Optional<Bounds> getCaretBoundsOnScreen(T caret);

    /**
     * Gets the character bounds on screen
     *
     * @param from the start position
     * @param to the end position
     * @return the bounds or {@link Optional#empty()} if line is not visible or the range is only a newline character.
     */
    Optional<Bounds> getCharacterBoundsOnScreen(int from, int to);

    /**
     * Returns the bounds of the paragraph if it is visible or {@link Optional#empty()} if it's not.
     *
     * The returned bounds object will always be within the bounds of the area. In other words, it takes
     * scrolling into account. Note: the bound's width will always equal the area's width, not necessarily
     * the paragraph's real width (if it's short and doesn't take up all of the area's provided horizontal space
     * or if it's long and spans outside of the area's width).
     *
     * @param visibleParagraphIndex the index in area's list of visible paragraphs.
     */
    public Bounds getVisibleParagraphBoundsOnScreen(int visibleParagraphIndex);

    /**
     * Returns the bounds of the paragraph if it is visible or {@link Optional#empty()} if it's not.
     *
     * The returned bounds object will always be within the bounds of the area. In other words, it takes
     * scrolling into account. Note: the bound's width will always equal the area's width, not necessarily
     * the paragraph's real width (if it's short and doesn't take up all of the area's provided horizontal space
     * or if it's long and spans outside of the area's width).
     *
     * @param paragraphIndex the index in area's list of paragraphs (visible and invisible).
     */
    public Optional<Bounds> getParagraphBoundsOnScreen(int paragraphIndex);

    /* ********************************************************************** *
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * Actions change the state of this control. They typically cause a       *
     * change of one or more observables and/or produce an event.             *
     *                                                                        *
     * ********************************************************************** */

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
     * Lays out the viewport so that the given bounds (according to the paragraph's coordinate system) within
     * the given paragraph is visible in the viewport.
     */
    void showParagraphRegion(int paragraphIndex, Bounds region);

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
    default void selectLine() {
        lineStart(NavigationActions.SelectionPolicy.CLEAR);
        lineEnd(NavigationActions.SelectionPolicy.ADJUST);
    };

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

    /**
     * Hides the area's context menu if it is not {@code null} and it is {@link ContextMenu#isShowing() showing}.
     */
    default void hideContextMenu() {
        ContextMenu menu = getContextMenu();
        if (menu != null && menu.isShowing()) {
            menu.hide();
        }
    }

}
