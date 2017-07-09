package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.StyledDocument;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableNo;
import org.reactfx.util.Tuple3;
import org.reactfx.util.Tuples;
import org.reactfx.value.SuspendableVal;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import java.util.Optional;

final class CaretSelectionBindImpl<PS, SEG, S> implements CaretSelectionBind<PS, SEG, S> {

    /* ********************************************************************** *
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     * Observables are "dynamic" (i.e. changing) characteristics of this      *
     * control. They are not directly settable by the client code, but change *
     * in response to user input and/or API actions.                          *
     *                                                                        *
     * ********************************************************************** */

    private final Selection<PS, SEG, S> delegate;
    @Override public ObservableValue<IndexRange> rangeProperty() { return delegate.rangeProperty(); }
    @Override public IndexRange getRange() { return delegate.getRange(); }

    @Override public ObservableValue<Integer> lengthProperty() { return delegate.lengthProperty(); }
    @Override public int getLength() { return delegate.getLength(); }

    @Override public ObservableValue<Integer> paragraphSpanProperty() { return delegate.paragraphSpanProperty(); }
    @Override public int getParagraphSpan() { return delegate.getParagraphSpan(); }

    @Override public final ObservableValue<StyledDocument<PS, SEG, S>> selectedDocumentProperty() { return delegate.selectedDocumentProperty(); }
    @Override public final StyledDocument<PS, SEG, S> getSelectedDocument() { return delegate.getSelectedDocument(); }

    @Override public ObservableValue<String> selectedTextProperty() { return delegate.selectedTextProperty(); }
    @Override public String getSelectedText() { return delegate.getSelectedText(); }


    @Override public ObservableValue<Integer> startPositionProperty() { return delegate.startPositionProperty(); }
    @Override public int getStartPosition() { return delegate.getStartPosition(); }

    @Override public ObservableValue<Integer> startParagraphIndexProperty() { return delegate.startParagraphIndexProperty(); }
    @Override public int getStartParagraphIndex() { return delegate.getStartParagraphIndex(); }

    @Override public ObservableValue<Integer> startColumnPositionProperty() { return delegate.startColumnPositionProperty(); }
    @Override public int getStartColumnPosition() { return delegate.getStartColumnPosition(); }


    @Override public ObservableValue<Integer> endPositionProperty() { return delegate.endPositionProperty(); }
    @Override public int getEndPosition() { return delegate.getEndPosition(); }

    @Override public ObservableValue<Integer> endPararagraphIndexProperty() { return delegate.endPararagraphIndexProperty(); }
    @Override public int getEndPararagraphIndex() { return delegate.getEndPararagraphIndex(); }

    @Override public ObservableValue<Integer> endColumnPositionProperty() { return delegate.endColumnPositionProperty(); }
    @Override public int getEndColumnPosition() { return delegate.getEndColumnPosition(); }


    private final Val<Integer> anchorPosition;
    @Override public int getAnchorPosition() { return anchorPosition.getValue(); }
    @Override public ObservableValue<Integer> anchorPositionProperty() { return anchorPosition; }

    private final Val<Integer> anchorParIndex;
    @Override public int getAnchorParIndex() { return anchorParIndex.getValue(); }
    @Override public ObservableValue<Integer> anchorParIndexProperty() { return anchorParIndex; }

    private final Val<Integer> anchorColPosition;
    @Override public int getAnchorColPosition() { return anchorColPosition.getValue(); }
    @Override public ObservableValue<Integer> anchorColPositionProperty() { return anchorColPosition; }

    @Override public ObservableValue<Optional<Bounds>> boundsProperty() { return delegate.boundsProperty(); }
    @Override public Optional<Bounds> getBounds() { return delegate.getBounds(); }

    @Override public EventStream<?> dirtyEvents() { return delegate.dirtyEvents(); }

    private final SuspendableNo beingUpdated = new SuspendableNo();
    public final boolean isBeingUpdated() { return beingUpdated.get(); }
    public final ObservableValue<Boolean> beingUpdatedProperty() { return beingUpdated; }

    private final Var<Boolean> internalStartedByAnchor = Var.newSimpleVar(true);
    private final SuspendableVal<Boolean> startedByAnchor = internalStartedByAnchor.suspendable();
    private boolean anchorIsStart() { return startedByAnchor.getValue(); }

    private final GenericStyledArea<PS, SEG, S> area;
    private final Caret caret;

    private Subscription subscription = () -> {};

    CaretSelectionBindImpl(GenericStyledArea<PS, SEG, S> area) {
        this(area, area.getMainCaret());
    }

    CaretSelectionBindImpl(GenericStyledArea<PS, SEG, S> area, Caret caret) {
        this(area, caret, new IndexRange(0, 0));
    }

    CaretSelectionBindImpl(GenericStyledArea<PS, SEG, S> area, Caret caret, IndexRange startingRange) {
        this.area = area;
        this.caret = caret;

        SuspendableNo delegateUpdater = new SuspendableNo();
        delegate = new SelectionImpl<>(area, delegateUpdater, startingRange);

        Val<Tuple3<Integer, Integer, Integer>> anchorPositions = startedByAnchor.flatMap(b ->
                b
                    ? Val.constant(Tuples.t(getStartPosition(), getStartParagraphIndex(), getStartColumnPosition()))
                    : Val.constant(Tuples.t(getEndPosition(), getEndPararagraphIndex(), getEndColumnPosition()))
        );

        anchorPosition = anchorPositions.map(Tuple3::get1);
        anchorParIndex = anchorPositions.map(Tuple3::get2);
        anchorColPosition = anchorPositions.map(Tuple3::get3);

        Suspendable omniSuspendable = Suspendable.combine(
                // first, so it's released last
                beingUpdated,

                startedByAnchor,

                // last, so it's released before startedByAnchor, so that anchor's values are correct
                delegateUpdater
        );

        subscription = omniSuspendable.suspendWhen(area.beingUpdatedProperty());
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
    public void selectRangeExpl(int anchorParagraph, int anchorColumn, int caretParagraph, int caretColumn) {
        selectRangeExpl(textPosition(anchorParagraph, anchorColumn), textPosition(caretParagraph, caretColumn));
    }

    @Override
    public void selectRangeExpl(int anchorPosition, int caretPosition) {
        if (anchorPosition <= caretPosition) {
            doSelect(anchorPosition, caretPosition, true);
        } else {
            doSelect(caretPosition, anchorPosition, false);
        }
    }

    @Override
    public void selectRange(int startPosition, int endPosition) {
        doSelect(startPosition, endPosition, anchorIsStart());
    }

    @Override
    public void selectRange(int startParagraphIndex, int startColPosition, int endParagraphIndex, int endColPosition) {
        selectRange(textPosition(startParagraphIndex, startColPosition), textPosition(endParagraphIndex, endColPosition));
    }

    @Override
    public void moveStartBy(int amount, Direction direction) {
        int updatedStart = direction == Direction.LEFT
                ? getStartPosition() - amount
                : getStartPosition() + amount;
        selectRange(updatedStart, getEndPosition());
    }

    @Override
    public void moveEndBy(int amount, Direction direction) {
        int updatedEnd = direction == Direction.LEFT
                ? getEndPosition() - amount
                : getEndPosition() + amount;
        selectRange(getStartPosition(), updatedEnd);
    }

    @Override
    public void moveStartTo(int position) {
        selectRange(position, getEndPosition());
    }

    @Override
    public void moveStartTo(int paragraphIndex, int columnPosition) {
        moveStartTo(textPosition(paragraphIndex, columnPosition));
    }

    @Override
    public void moveEndTo(int position) {
        selectRange(getStartPosition(), position);
    }

    @Override
    public void moveEndTo(int paragraphIndex, int columnPosition) {
        moveEndTo(textPosition(paragraphIndex, columnPosition));
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

    private void doSelect(int startPosition, int endPosition, boolean anchorIsStart) {
        Runnable updateRange = () -> {
            delegate.selectRange(startPosition, endPosition);
            internalStartedByAnchor.setValue(anchorIsStart);

            caret.moveTo(anchorIsStart ? endPosition : startPosition);
        };

        if (area.isBeingUpdated()) {
            updateRange.run();
        } else {
            area.beingUpdatedProperty().suspendWhile(updateRange);
        }
    }

    private int textPosition(int paragraphIndex, int columnPosition) {
        return area.position(paragraphIndex, columnPosition).toOffset();
    }

}
