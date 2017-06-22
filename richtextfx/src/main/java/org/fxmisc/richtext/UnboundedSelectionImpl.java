package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.TwoDimensional.Position;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableNo;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuples;
import org.reactfx.value.SuspendableVal;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.Backward;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.Forward;
import static org.reactfx.EventStreams.invalidationsOf;
import static org.reactfx.EventStreams.merge;

final class UnboundedSelectionImpl<PS, SEG, S> implements UnboundedSelection<PS, SEG, S> {

    private final SuspendableVal<IndexRange> range;
    @Override public final IndexRange getRange() { return range.getValue(); }
    @Override public final ObservableValue<IndexRange> rangeProperty() { return range; }

    private final SuspendableVal<Integer> length;
    @Override public final int getLength() { return length.getValue(); }
    @Override public final ObservableValue<Integer> lengthProperty() { return length; }

    private final SuspendableVal<Integer> paragraphSpan;
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

    private final SuspendableVal<Integer> startParagraphIndex;
    @Override public final int getStartParagraphIndex() { return startParagraphIndex.getValue(); }
    @Override public final ObservableValue<Integer> startParagraphIndexProperty() { return startParagraphIndex; }

    private final SuspendableVal<Integer> startColumnPosition;
    @Override public final int getStartColumnPosition() { return startColumnPosition.getValue(); }
    @Override public final ObservableValue<Integer> startColumnPositionProperty() { return startColumnPosition; }


    private final SuspendableVal<Integer> endPosition;
    @Override public final int getEndPosition() { return endPosition.getValue(); }
    @Override public final ObservableValue<Integer> endPositionProperty() { return endPosition; }

    private final SuspendableVal<Integer> endPararagraphIndex;
    @Override public final int getEndPararagraphIndex() { return endPararagraphIndex.getValue(); }
    @Override public final ObservableValue<Integer> endPararagraphIndexProperty() { return endPararagraphIndex; }

    private final SuspendableVal<Integer> endColumnPosition;
    @Override public final int getEndColumnPosition() { return endColumnPosition.getValue(); }
    @Override public final ObservableValue<Integer> endColumnPositionProperty() { return endColumnPosition; }


    private final Val<Optional<Bounds>> bounds;
    @Override public final Optional<Bounds> getBounds() { return bounds.getValue(); }
    @Override public final ObservableValue<Optional<Bounds>> boundsProperty() { return bounds; }

    private final EventStream<?> dirty;
    @Override public final EventStream<?> dirtyEvents() { return dirty; }

    private final SuspendableNo beingUpdated = new SuspendableNo();
    @Override public final boolean isBeingUpdated() { return beingUpdated.get(); }
    @Override public final ObservableValue<Boolean> beingUpdatedProperty() { return beingUpdated; }

    private final GenericStyledArea<PS, SEG, S> area;
    private final SuspendableNo dependentBeingUpdated;
    private final Var<IndexRange> internalRange;

    private Subscription subscription = () -> {};

    public UnboundedSelectionImpl(GenericStyledArea<PS, SEG, S> area) {
        this(area, 0, 0);
    }

    public UnboundedSelectionImpl(GenericStyledArea<PS, SEG, S> area, int startPosition, int endPosition) {
        this(area, area.beingUpdatedProperty(), new IndexRange(startPosition, endPosition));
    }

    public UnboundedSelectionImpl(GenericStyledArea<PS, SEG, S> area, SuspendableNo dependentBeingUpdated, int startPosition, int endPosition) {
        this(area, dependentBeingUpdated, new IndexRange(startPosition, endPosition));
    }

    public UnboundedSelectionImpl(GenericStyledArea<PS, SEG, S> area, SuspendableNo dependentBeingUpdated, IndexRange range) {
        this.area = area;
        this.dependentBeingUpdated = dependentBeingUpdated;
        internalRange = Var.newSimpleVar(range);

        this.range = internalRange.suspendable();
        length = internalRange.map(IndexRange::getLength).suspendable();

        selectedText = Val.create(() -> area.getText(internalRange.getValue()),
                internalRange, area.getParagraphs()
        ).suspendable();

        selectedDocument = Val.create(() -> area.subDocument(internalRange.getValue()),
                internalRange, area.getParagraphs()
        ).suspendable();

        Val<Tuple2<Position, Position>> positions = internalRange.map(sel -> {
            Position start2D = area.offsetToPosition(sel.getStart(), Forward);
            Position end2D = sel.getLength() == 0
                    ? start2D
                    : start2D.offsetBy(sel.getLength(), Backward);
            return Tuples.t(start2D, end2D);
        });

        startPosition = internalRange.map(IndexRange::getStart).suspendable();

        Val<Position> start2D = positions.map(Tuple2::get1);
        Val<Integer> startPar = start2D.map(Position::getMajor);
        startParagraphIndex = startPar.suspendable();
        startColumnPosition = start2D.map(Position::getMinor).suspendable();

        endPosition = internalRange.map(IndexRange::getEnd).suspendable();

        Val<Position> end2D = positions.map(Tuple2::get2);
        Val<Integer> endPar = end2D.map(Position::getMajor);
        endPararagraphIndex = endPar.suspendable();
        endColumnPosition = end2D.map(Position::getMinor).suspendable();

        paragraphSpan = Val.create(
                () -> getEndPararagraphIndex() - getStartParagraphIndex() + 1,
                startPar, endPar
        ).suspendable();

        dirty = merge(
                invalidationsOf(rangeProperty()),
                invalidationsOf(area.getParagraphs())
        );

        bounds = Val.create(
                () -> area.impl_bounds_getSelectionBoundsOnScreen(this),
                area.boundsDirtyFor(dirty)
        );

        manageSubscription(area.plainTextChanges(), plainTextChange -> {
            int changeLength = plainTextChange.getInserted().length() - plainTextChange.getRemoved().length();
            if (changeLength != 0) {
                int indexOfChange = plainTextChange.getPosition();
                // in case of a replacement: "hello there" -> "hi."
                int endOfChange = indexOfChange + Math.abs(changeLength);

                if (getLength() != 0) {
                    int selectionStart = getStartPosition();
                    int selectionEnd = getEndPosition();

                    // if start/end is within the changed content, move it to indexOfChange
                    // otherwise, offset it by changeLength
                    // Note: if both are moved to indexOfChange, selection is empty.
                    if (indexOfChange < selectionStart) {
                        selectionStart = selectionStart < endOfChange
                                ? indexOfChange
                                : selectionStart + changeLength;
                    }
                    if (indexOfChange < selectionEnd) {
                        selectionEnd = selectionEnd < endOfChange
                                ? indexOfChange
                                : selectionEnd + changeLength;
                    }
                    selectRange0(selectionStart, selectionEnd);
                } else {
                    // force-update internalSelection in case empty selection is
                    // at the end of area and a character was deleted
                    // (prevents a StringIndexOutOfBoundsException because
                    // end is one char farther than area's length).

                    if (getLength() < getEndPosition()) {
                        selectRange0(getLength(), getLength());
                    }
                }
            }
        });

        Suspendable omniSuspendable = Suspendable.combine(
                // first, so it's released last
                beingUpdated,

                paragraphSpan,

                endColumnPosition,
                endPararagraphIndex,
                endPosition,

                startColumnPosition,
                startParagraphIndex,
                startPosition,

                selectedText,
                selectedDocument,
                length,
                this.range
        );
        manageSubscription(omniSuspendable.suspendWhen(dependentBeingUpdated));
    }

    @Override
    public void selectRange0(int startParagraphIndex, int startColPosition, int endParagraphIndex, int endColPosition) {
        selectRange0(textPosition(startParagraphIndex, startColPosition), textPosition(endParagraphIndex, endColPosition));
    }

    @Override
    public void selectRange0(int startPosition, int endPosition) {
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
    public void moveStartBy(int amount, Direction direction) {
        moveBoundary(direction, amount, getStartPosition(),
                newStartTextPos -> IndexRange.normalize(newStartTextPos, getEndPosition())
        );
    }

    @Override
    public void moveEndBy(int amount, Direction direction) {
        moveBoundary(
                direction, amount, getEndPosition(),
                newEndTextPos -> IndexRange.normalize(getStartPosition(), newEndTextPos)
        );
    }

    @Override
    public void moveStartTo(int position) {
        selectRange0(position, getEndPosition());
    }

    @Override
    public void moveStartTo(int paragraphIndex, int columnPosition) {
        selectRange0(textPosition(paragraphIndex, columnPosition), getEndPosition());
    }

    @Override
    public void moveEndTo(int position) {
        selectRange0(getStartPosition(), position);
    }

    @Override
    public void moveEndTo(int paragraphIndex, int columnPosition) {
        selectRange0(getStartPosition(), textPosition(paragraphIndex, columnPosition));
    }

    @Override
    public void dispose() {
        subscription.unsubscribe();
    }

    private <T> void manageSubscription(EventStream<T> stream, Consumer<T> consumer) {
        manageSubscription(stream.subscribe(consumer));
    }

    private void manageSubscription(Subscription s) {
        subscription = subscription.and(s);
    }

    private Position position(int row, int col) {
        return area.position(row, col);
    }

    private int textPosition(int row, int col) {
        return position(row, col).toOffset();
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

}
