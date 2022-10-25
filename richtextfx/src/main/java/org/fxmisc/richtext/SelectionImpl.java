package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.TwoDimensional.Position;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableNo;
import org.reactfx.value.SuspendableVal;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import java.text.BreakIterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.Backward;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.Forward;
import static org.reactfx.EventStreams.invalidationsOf;
import static org.reactfx.EventStreams.merge;

/**
 * Default implementation for {@link Selection}.
 */
public class SelectionImpl<PS, SEG, S> implements Selection<PS, SEG, S>, Comparable<SelectionImpl<PS, SEG, S>> {

    /* ********************************************************************** *
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     * Observables are "dynamic" (i.e. changing) characteristics of this      *
     * control. They are not directly settable by the client code, but change *
     * in response to user input and/or API actions.                          *
     *                                                                        *
     * ********************************************************************** */

    private final SuspendableVal<IndexRange> range;
    @Override public final IndexRange getRange() { return range.getValue(); }
    @Override public final ObservableValue<IndexRange> rangeProperty() { return range; }

    private final SuspendableVal<Integer> length;
    @Override public final int getLength() { return length.getValue(); }
    @Override public final ObservableValue<Integer> lengthProperty() { return length; }

    private final Val<Integer> paragraphSpan;
    @Override public final int getParagraphSpan() { return paragraphSpan.getValue(); }
    @Override public final ObservableValue<Integer> paragraphSpanProperty() { return paragraphSpan; }

    private final SuspendableVal<StyledDocument<PS, SEG, S>> selectedDocument;
    @Override public final ObservableValue<StyledDocument<PS, SEG, S>> selectedDocumentProperty() { return selectedDocument; }
    @Override public final StyledDocument<PS, SEG, S> getSelectedDocument() { return selectedDocument.getValue(); }

    private final SuspendableVal<String> selectedText;
    @Override public final String getSelectedText() { return selectedText.getValue(); }
    @Override public final ObservableValue<String> selectedTextProperty() { return selectedText; }


    private final SuspendableVal<Integer> startPosition;
    @Override public final int getStartPosition() { return startPosition.getValue(); }
    @Override public final ObservableValue<Integer> startPositionProperty() { return startPosition; }

    private final Val<Integer> startParagraphIndex;
    @Override public final int getStartParagraphIndex() { return startParagraphIndex.getValue(); }
    @Override public final ObservableValue<Integer> startParagraphIndexProperty() { return startParagraphIndex; }

    private final Val<Integer> startColumnPosition;
    @Override public final int getStartColumnPosition() { return startColumnPosition.getValue(); }
    @Override public final ObservableValue<Integer> startColumnPositionProperty() { return startColumnPosition; }


    private final SuspendableVal<Integer> endPosition;
    @Override public final int getEndPosition() { return endPosition.getValue(); }
    @Override public final ObservableValue<Integer> endPositionProperty() { return endPosition; }

    private final Val<Integer> endParagraphIndex;
    @Override public final int getEndParagraphIndex() { return endParagraphIndex.getValue(); }
    @Override public final ObservableValue<Integer> endParagraphIndexProperty() { return endParagraphIndex; }

    private final Val<Integer> endColumnPosition;
    @Override public final int getEndColumnPosition() { return endColumnPosition.getValue(); }
    @Override public final ObservableValue<Integer> endColumnPositionProperty() { return endColumnPosition; }


    private final SuspendableVal<Optional<Bounds>> bounds;
    @Override public final Optional<Bounds> getSelectionBounds() { return bounds.getValue(); }
    @Override public final ObservableValue<Optional<Bounds>> selectionBoundsProperty() { return bounds; }

    private final SuspendableNo beingUpdated = new SuspendableNo();
    @Override public final boolean isBeingUpdated() { return beingUpdated.get(); }
    @Override public final ObservableValue<Boolean> beingUpdatedProperty() { return beingUpdated; }

    private final GenericStyledArea<PS, SEG, S> area;
    @Override public GenericStyledArea<PS, SEG, S> getArea() { return area; }

    private final String name;
    @Override public String getSelectionName() { return name; }

    private final SuspendableNo dependentBeingUpdated;
    private final Var<IndexRange> internalRange;
    private final EventStream<?> dirty;

    private final Var<Position> start2DPosition;
    private final Val<Position> end2DPosition;

    private final Consumer<SelectionPath> configurePath;

    private Subscription subscription = () -> {};

    /**
     * Creates a selection with both the start and end position at 0.
     * @param name must be unique and is also used as a StyleClass for
     * configuration via CSS using selectors from Path, Shape, and Node.
     */
    public SelectionImpl(String name, GenericStyledArea<PS, SEG, S> area) {
        this(name, area, 0, 0);
    }

    /**
     * Creates a selection with customized configuration via {@code configurePath}
     * with both the start and end position at 0.
     */
    public SelectionImpl(String name, GenericStyledArea<PS, SEG, S> area, Consumer<SelectionPath> configurePath) {
        this(name, area, 0, 0, area.beingUpdatedProperty(), configurePath);
    }

    /**
     * Creates a selection. Name must be unique and is also used as a StyleClass
     * for configuration via CSS using selectors from Path, Shape, and Node. 
     */
    public SelectionImpl(String name, GenericStyledArea<PS, SEG, S> area, int startPosition, int endPosition) {
        this(name, area, new IndexRange(startPosition, endPosition), area.beingUpdatedProperty());
    }

    /**
     * Creates a selection that is to be used in a {@link CaretSelectionBind}.
     */
    SelectionImpl(String name, GenericStyledArea<PS, SEG, S> area, int startPosition, int endPosition,
                         SuspendableNo dependentBeingUpdated) {
        this(name, area, new IndexRange(startPosition, endPosition), dependentBeingUpdated);
    }

    /**
     * Creates a selection that is to be used in a {@link CaretSelectionBind} with customized configuration.
     */
    SelectionImpl(String name, GenericStyledArea<PS, SEG, S> area, int startPosition, int endPosition,
                         SuspendableNo dependentBeingUpdated, Consumer<SelectionPath> configurePath) {
        this(name, area, new IndexRange(startPosition, endPosition), dependentBeingUpdated, configurePath);
    }

    /**
     * Creates a selection that is to be used in a {@link CaretSelectionBind}. It adds the style class
     * {@code selection} to any {@link SelectionPath} used to render this selection.
     */
    SelectionImpl(String name, GenericStyledArea<PS, SEG, S> area, IndexRange range,
                         SuspendableNo dependentBeingUpdated) {
        this(name, area, range, dependentBeingUpdated, path -> path.getStyleClass().add("selection"));
    }

    /**
     * Creates a selection that is to be used in a {@link CaretSelectionBind}
     * with customized configuration and starting at the given range.
     */
    SelectionImpl(String name, GenericStyledArea<PS, SEG, S> area, IndexRange range,
                         SuspendableNo dependentBeingUpdated, Consumer<SelectionPath> configurePath) {
        this.name = name;
        this.area = area;
        this.dependentBeingUpdated = dependentBeingUpdated;
        this.configurePath = configurePath;
        internalRange = Var.newSimpleVar(range);

        this.range = internalRange.suspendable();
        length = internalRange.map(IndexRange::getLength).suspendable();

        Val<StyledDocument<PS, SEG, S>> documentVal = Val.create(
                () -> area.subDocument(internalRange.getValue()),
                internalRange, area.getParagraphs()
        );
        selectedDocument = documentVal.suspendable();
        selectedText = documentVal.map(StyledDocument::getText).suspendable();

        start2DPosition = Var.newSimpleVar(area.offsetToPosition(range.getStart(), Forward));
        end2DPosition = start2DPosition.map(startPos2D ->
                getLength() == 0
                        ? startPos2D
                        : startPos2D.offsetBy(getLength(), Backward)
        );

        internalRange.addListener(obs -> {
            IndexRange sel = internalRange.getValue();
            start2DPosition.setValue(area.offsetToPosition(sel.getStart(), Forward));
        });

        startPosition = internalRange.map(IndexRange::getStart).suspendable();
        startParagraphIndex = start2DPosition.map(Position::getMajor);
        startColumnPosition = start2DPosition.map(Position::getMinor);

        endPosition = internalRange.map(IndexRange::getEnd).suspendable();
        endParagraphIndex = end2DPosition.map(Position::getMajor);
        endColumnPosition = end2DPosition.map(Position::getMinor);

        paragraphSpan = Val.combine(
                startParagraphIndex, endParagraphIndex,
                (startP, endP) -> endP - startP + 1
        );

        dirty = merge(
                invalidationsOf(rangeProperty()),
                invalidationsOf(area.getParagraphs())
        );

        bounds = Val.create(
                () -> area.getSelectionBoundsOnScreen(this),
                EventStreams.merge(area.viewportDirtyEvents(), dirty)
        ).suspendable();

        manageSubscription(area.multiPlainChanges(), list -> {
            int selectStart = getStartPosition();
            int selectEnd = getEndPosition();
            for (PlainTextChange plainTextChange : list) {
                int changeLength = plainTextChange.getNetLength();
                int indexOfChange = plainTextChange.getPosition();
                // in case of a replacement: "hello there" -> "hi."
                int endOfChange = indexOfChange + Math.abs(changeLength);

                /*
                    "->" means add (positive) netLength to position
                    "<-" means add (negative) netLength to position
                    "x" means don't update position

                    "start / end" means what should be done in each case for each anchor if they differ

                    "+a" means one of the anchors was included in the deleted portion of content
                    "-a" means one of the anchors was not included in the deleted portion of content
                    Before/At/After means indexOfChange "<" / "==" / ">" position

                           |   Before +a   | Before -a |   At   | After
                    -------+---------------+-----------+--------+------
                    Add    |      N/A      |    ->     | -> / x | x
                    Delete | indexOfChange |    <-     |    x   | x
                */
                if (indexOfChange == selectStart && changeLength > 0) {
                    selectStart = selectStart + changeLength;
                } else if (indexOfChange < selectStart) {
                    selectStart = selectStart < endOfChange
                            ? indexOfChange
                            : selectStart + changeLength;
                }
                if (indexOfChange < selectEnd) {
                    selectEnd = selectEnd < endOfChange
                            ? indexOfChange
                            : selectEnd + changeLength;
                }
                if (selectStart > selectEnd) {
                    selectStart = selectEnd;
                }
            }
            selectRange(selectStart, selectEnd);
        });

        Suspendable omniSuspendable = Suspendable.combine(
                // first, so it's released last
                beingUpdated,

                bounds,

                endPosition,
                startPosition,

                selectedText,
                selectedDocument,
                length,
                this.range
        );
        manageSubscription(omniSuspendable.suspendWhen(dependentBeingUpdated));
    }

    /* ********************************************************************** *
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * Actions change the state of this control. They typically cause a       *
     * change of one or more observables and/or produce an event.             *
     *                                                                        *
     * ********************************************************************** */

    @Override
    public void selectRange(int startParagraphIndex, int startColPosition, int endParagraphIndex, int endColPosition) {
        selectRange(textPosition(startParagraphIndex, startColPosition), textPosition(endParagraphIndex, endColPosition));
    }

    @Override
    public void selectRange(int startPosition, int endPosition) {
        selectRange(new IndexRange(startPosition, endPosition));
    }

    private void selectRange(IndexRange range) {
        Runnable updateRange = () -> internalRange.setValue(range);
        if (dependentBeingUpdated.get()) {
            updateRange.run();
        } else {
            dependentBeingUpdated.suspendWhile(updateRange);
        }
    }

    @Override
    public void updateStartBy(int amount, Direction direction) {
        moveBoundary(direction, amount, getStartPosition(),
                newStartTextPos -> IndexRange.normalize(newStartTextPos, getEndPosition())
        );
    }

    @Override
    public void updateEndBy(int amount, Direction direction) {
        moveBoundary(
                direction, amount, getEndPosition(),
                newEndTextPos -> IndexRange.normalize(getStartPosition(), newEndTextPos)
        );
    }

    @Override
    public void updateStartTo(int position) {
        selectRange(position, getEndPosition());
    }

    @Override
    public void updateStartTo(int paragraphIndex, int columnPosition) {
        selectRange(textPosition(paragraphIndex, columnPosition), getEndPosition());
    }

    @Override
    public void updateStartByBreaksForward(int numOfBreaks, BreakIterator breakIterator) {
        updateStartByBreaks(numOfBreaks, breakIterator, true);
    }

    @Override
    public void updateStartByBreaksBackward(int numOfBreaks, BreakIterator breakIterator) {
        updateStartByBreaks(numOfBreaks, breakIterator, false);
    }

    @Override
    public void updateEndTo(int position) {
        selectRange(getStartPosition(), position);
    }

    @Override
    public void updateEndTo(int paragraphIndex, int columnPosition) {
        selectRange(getStartPosition(), textPosition(paragraphIndex, columnPosition));
    }

    @Override
    public void updateEndByBreaksForward(int numOfBreaks, BreakIterator breakIterator) {
        updateEndByBreaks(numOfBreaks, breakIterator, true);
    }

    @Override
    public void updateEndByBreaksBackward(int numOfBreaks, BreakIterator breakIterator) {
        updateEndByBreaks(numOfBreaks, breakIterator, false);
    }

    @Override
    public void selectAll() {
        selectRange(0, area.getLength());
    }

    @Override
    public void selectParagraph(int paragraphIndex) {
        int start = textPosition(paragraphIndex, 0);
        int end = start + area.getParagraphLength(paragraphIndex);
        selectRange(start, end);
    }

    @Override
    public void selectWord(int wordPositionInArea) {
        if(area.getLength() == 0) {
            return;
        }

        BreakIterator breakIterator = BreakIterator.getWordInstance( getArea().getLocale() );
        breakIterator.setText(area.getText());
        breakIterator.preceding(wordPositionInArea);
        breakIterator.next();
        int wordStart = breakIterator.current();

        breakIterator.following(wordPositionInArea);
        breakIterator.next();
        int wordEnd = breakIterator.current();

        selectRange(wordStart, wordEnd);
    }

    @Override
    public void configureSelectionPath(SelectionPath path) {
        configurePath.accept(path);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(SelectionImpl<PS, SEG, S> o) {
        return Integer.compare(hashCode(), o.hashCode());
    }

    @Override
    public String toString() {
        return String.format("SelectionImpl(name=%s startPar=%s startCol=%s " +
                        "endPar=%s endCol=%s paragraphSpan=%s " +
                        "selectedDocument=%s",
                name, getStartParagraphIndex(), getStartColumnPosition(),
                getEndParagraphIndex(), getEndColumnPosition(), getParagraphSpan(),
                getSelectedDocument());
    }

    @Override
    public void dispose() {
        subscription.unsubscribe();
    }

    /* ********************************************************************** *
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     * ********************************************************************** */

    private <T> void manageSubscription(EventStream<T> stream, Consumer<T> consumer) {
        manageSubscription(stream.subscribe(consumer));
    }

    private void manageSubscription(Subscription s) {
        subscription = subscription.and(s);
    }

    private int textPosition(int row, int col) {
        return area.position(row, col).toOffset();
    }

    private void moveBoundary(Direction direction, int amount, int oldBoundaryPosition,
                              Function<Integer, IndexRange> updatedRange) {
        switch (direction) {
            case LEFT:
                moveBoundary(
                        () -> oldBoundaryPosition - amount,
                        (pos) -> 0 <= pos,
                        updatedRange
                );
                break;
            default: case RIGHT:
                moveBoundary(
                        () -> oldBoundaryPosition + amount,
                        (pos) -> pos <= area.getLength(),
                        updatedRange
                );
        }
    }

    private void moveBoundary(IntSupplier textPosition, Function<Integer, Boolean> boundsCheckPasses,
                              Function<Integer, IndexRange> updatedRange) {
        int newTextPosition = textPosition.getAsInt();
        if (boundsCheckPasses.apply(newTextPosition)) {
            selectRange(updatedRange.apply(newTextPosition));
        }
    }

    private void updateStartByBreaks(int numOfBreaks, BreakIterator breakIterator, boolean forwardsNotBackwards) {
        updateSelectionByBreaks(numOfBreaks, breakIterator, forwardsNotBackwards, true);
    }

    private void updateEndByBreaks(int numOfBreaks, BreakIterator breakIterator, boolean forwardsNotBackwards) {
        updateSelectionByBreaks(numOfBreaks, breakIterator, forwardsNotBackwards, false);
    }

    private void updateSelectionByBreaks(int numOfBreaks, BreakIterator breakIterator,
                                         boolean followingNotPreceding, boolean updateStartNotEnd) {
        if (area.getLength() == 0) {
            return;
        }

        breakIterator.setText(area.getText());

        int pos;
        Runnable updateSelection;
        if (updateStartNotEnd) {
            pos = getStartPosition();
            updateSelection = () -> selectRange(breakIterator.current(), getEndPosition());
        } else {
            pos = getEndPosition();
            updateSelection = () -> selectRange(getStartPosition(), breakIterator.current());
        }

        if (followingNotPreceding) {
            breakIterator.following(pos);
        } else {
            breakIterator.preceding(pos);
        }
        breakIterator.next(numOfBreaks);

        updateSelection.run();
    }

}
