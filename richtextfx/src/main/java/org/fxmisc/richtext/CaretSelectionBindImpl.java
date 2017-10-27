package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.StyledDocument;
import org.reactfx.Subscription;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableNo;
import org.reactfx.util.Tuple3;
import org.reactfx.util.Tuples;
import org.reactfx.value.SuspendableVal;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import java.text.BreakIterator;
import java.util.Optional;
import java.util.OptionalInt;

final class CaretSelectionBindImpl<PS, SEG, S> implements CaretSelectionBind<PS, SEG, S> {

    // caret
    @Override public Var<CaretVisibility> showCaretProperty() { return delegateCaret.showCaretProperty(); }
    @Override public CaretVisibility getShowCaret() { return delegateCaret.getShowCaret(); }
    @Override public void setShowCaret(CaretVisibility value) { delegateCaret.setShowCaret(value); }

    /* ********************************************************************** *
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     * Observables are "dynamic" (i.e. changing) characteristics of this      *
     * control. They are not directly settable by the client code, but change *
     * in response to user input and/or API actions.                          *
     *                                                                        *
     * ********************************************************************** */

    // caret
    @Override public ObservableValue<Integer> positionProperty() { return delegateCaret.positionProperty(); }
    @Override public int getPosition() { return delegateCaret.getPosition(); }

    @Override public ObservableValue<Integer> paragraphIndexProperty() { return delegateCaret.paragraphIndexProperty(); }
    @Override public int getParagraphIndex() { return delegateCaret.getParagraphIndex(); }

    @Override public ObservableValue<OptionalInt> lineIndexProperty() { return delegateCaret.lineIndexProperty(); }
    @Override public OptionalInt getLineIndex() { return delegateCaret.getLineIndex(); }

    @Override public ObservableValue<Integer> columnPositionProperty() { return delegateCaret.columnPositionProperty(); }
    @Override public int getColumnPosition() { return delegateCaret.getColumnPosition(); }

    @Override public ObservableValue<Boolean> visibleProperty() { return delegateCaret.visibleProperty(); }
    @Override public boolean isVisible() { return delegateCaret.isVisible(); }

    @Override public ObservableValue<Optional<Bounds>> caretBoundsProperty() { return delegateCaret.caretBoundsProperty(); }
    @Override public Optional<Bounds> getCaretBounds() { return delegateCaret.getCaretBounds(); }

    @Override public void clearTargetOffset() { delegateCaret.clearTargetOffset(); }
    @Override public ParagraphBox.CaretOffsetX getTargetOffset() { return delegateCaret.getTargetOffset(); }

    // selection
    @Override public ObservableValue<IndexRange> rangeProperty() { return delegateSelection.rangeProperty(); }
    @Override public IndexRange getRange() { return delegateSelection.getRange(); }

    @Override public ObservableValue<Integer> lengthProperty() { return delegateSelection.lengthProperty(); }
    @Override public int getLength() { return delegateSelection.getLength(); }

    @Override public ObservableValue<Integer> paragraphSpanProperty() { return delegateSelection.paragraphSpanProperty(); }
    @Override public int getParagraphSpan() { return delegateSelection.getParagraphSpan(); }

    @Override public final ObservableValue<StyledDocument<PS, SEG, S>> selectedDocumentProperty() { return delegateSelection.selectedDocumentProperty(); }
    @Override public final StyledDocument<PS, SEG, S> getSelectedDocument() { return delegateSelection.getSelectedDocument(); }

    @Override public ObservableValue<String> selectedTextProperty() { return delegateSelection.selectedTextProperty(); }
    @Override public String getSelectedText() { return delegateSelection.getSelectedText(); }


    @Override public ObservableValue<Integer> startPositionProperty() { return delegateSelection.startPositionProperty(); }
    @Override public int getStartPosition() { return delegateSelection.getStartPosition(); }

    @Override public ObservableValue<Integer> startParagraphIndexProperty() { return delegateSelection.startParagraphIndexProperty(); }
    @Override public int getStartParagraphIndex() { return delegateSelection.getStartParagraphIndex(); }

    @Override public ObservableValue<Integer> startColumnPositionProperty() { return delegateSelection.startColumnPositionProperty(); }
    @Override public int getStartColumnPosition() { return delegateSelection.getStartColumnPosition(); }


    @Override public ObservableValue<Integer> endPositionProperty() { return delegateSelection.endPositionProperty(); }
    @Override public int getEndPosition() { return delegateSelection.getEndPosition(); }

    @Override public ObservableValue<Integer> endParagraphIndexProperty() { return delegateSelection.endParagraphIndexProperty(); }
    @Override public int getEndParagraphIndex() { return delegateSelection.getEndParagraphIndex(); }

    @Override public ObservableValue<Integer> endColumnPositionProperty() { return delegateSelection.endColumnPositionProperty(); }
    @Override public int getEndColumnPosition() { return delegateSelection.getEndColumnPosition(); }

    @Override public ObservableValue<Optional<Bounds>> selectionBoundsProperty() { return delegateSelection.selectionBoundsProperty(); }
    @Override public Optional<Bounds> getSelectionBounds() { return delegateSelection.getSelectionBounds(); }

    // caret selection bind
    private final Val<Integer> anchorPosition;
    @Override public int getAnchorPosition() { return anchorPosition.getValue(); }
    @Override public ObservableValue<Integer> anchorPositionProperty() { return anchorPosition; }

    private final Val<Integer> anchorParIndex;
    @Override public int getAnchorParIndex() { return anchorParIndex.getValue(); }
    @Override public ObservableValue<Integer> anchorParIndexProperty() { return anchorParIndex; }

    private final Val<Integer> anchorColPosition;
    @Override public int getAnchorColPosition() { return anchorColPosition.getValue(); }
    @Override public ObservableValue<Integer> anchorColPositionProperty() { return anchorColPosition; }

    private final SuspendableNo beingUpdated = new SuspendableNo();
    public final boolean isBeingUpdated() { return beingUpdated.get(); }
    public final ObservableValue<Boolean> beingUpdatedProperty() { return beingUpdated; }

    private final Var<Boolean> internalStartedByAnchor = Var.newSimpleVar(true);
    private final SuspendableVal<Boolean> startedByAnchor = internalStartedByAnchor.suspendable();
    private boolean anchorIsStart() { return startedByAnchor.getValue(); }

    private final GenericStyledArea<PS, SEG, S> area;
    private final Caret delegateCaret;
    private final Selection<PS, SEG, S> delegateSelection;

    private Subscription subscription = () -> {};

    CaretSelectionBindImpl(GenericStyledArea<PS, SEG, S> area) {
        this(area, new IndexRange(0, 0));
    }

    CaretSelectionBindImpl(GenericStyledArea<PS, SEG, S> area, IndexRange startingRange) {
        this.area = area;
        SuspendableNo delegateUpdater = new SuspendableNo();
        this.delegateCaret = new CaretImpl(area, delegateUpdater, startingRange.getStart());
        delegateSelection = new SelectionImpl<>(area, delegateUpdater, startingRange);

        Val<Tuple3<Integer, Integer, Integer>> anchorPositions = startedByAnchor.flatMap(b ->
                b
                    ? Val.constant(Tuples.t(getStartPosition(), getStartParagraphIndex(), getStartColumnPosition()))
                    : Val.constant(Tuples.t(getEndPosition(), getEndParagraphIndex(), getEndColumnPosition()))
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

    // caret
    @Override
    public void moveBreaksForwards(int numOfBreaks, BreakIterator breakIterator) {
        if (getAreaLength() == 0) {
            return;
        }

        breakIterator.setText(area.getText());
        int position = calculatePositionViaBreakingForwards(numOfBreaks, breakIterator, getPosition());
        moveTo(position, NavigationActions.SelectionPolicy.CLEAR);
    }

    @Override
    public void moveBreaksBackwards(int numOfBreaks, BreakIterator breakIterator) {
        if (getAreaLength() == 0) {
            return;
        }

        breakIterator.setText(area.getText());
        int position = calculatePositionViaBreakingBackwards(numOfBreaks, breakIterator, getPosition());
        moveTo(position, NavigationActions.SelectionPolicy.CLEAR);
    }

    // selection
    @Override
    public void selectRange(int startPosition, int endPosition) {
        doSelect(startPosition, endPosition, anchorIsStart());
    }

    @Override
    public void selectRange(int startParagraphIndex, int startColPosition, int endParagraphIndex, int endColPosition) {
        selectRange(textPosition(startParagraphIndex, startColPosition), textPosition(endParagraphIndex, endColPosition));
    }

    @Override
    public void updateStartBy(int amount, Direction direction) {
        int updatedStart = direction == Direction.LEFT
                ? getStartPosition() - amount
                : getStartPosition() + amount;
        selectRange(updatedStart, getEndPosition());
    }

    @Override
    public void updateEndBy(int amount, Direction direction) {
        int updatedEnd = direction == Direction.LEFT
                ? getEndPosition() - amount
                : getEndPosition() + amount;
        selectRange(getStartPosition(), updatedEnd);
    }

    @Override
    public void updateStartTo(int position) {
        selectRange(position, getEndPosition());
    }

    @Override
    public void updateStartTo(int paragraphIndex, int columnPosition) {
        updateStartTo(textPosition(paragraphIndex, columnPosition));
    }

    @Override
    public void updateStartByBreaksForward(int numOfBreaks, BreakIterator breakIterator) {
        if (getAreaLength() == 0) {
            return;
        }

        breakIterator.setText(area.getText());
        int position = calculatePositionViaBreakingForwards(numOfBreaks, breakIterator, getStartPosition());
        updateStartTo(position);
    }

    @Override
    public void updateStartByBreaksBackward(int numOfBreaks, BreakIterator breakIterator) {
        if (getAreaLength() == 0) {
            return;
        }

        breakIterator.setText(area.getText());
        int position = calculatePositionViaBreakingBackwards(numOfBreaks, breakIterator, getStartPosition());
        updateStartTo(position);
    }

    @Override
    public void updateEndTo(int position) {
        selectRange(getStartPosition(), position);
    }

    @Override
    public void updateEndTo(int paragraphIndex, int columnPosition) {
        updateEndTo(textPosition(paragraphIndex, columnPosition));
    }

    @Override
    public void updateEndByBreaksForward(int numOfBreaks, BreakIterator breakIterator) {
        if (getAreaLength() == 0) {
            return;
        }

        breakIterator.setText(area.getText());
        int position = calculatePositionViaBreakingForwards(numOfBreaks, breakIterator, getStartPosition());
        updateEndTo(position);
    }

    @Override
    public void updateEndByBreaksBackward(int numOfBreaks, BreakIterator breakIterator) {
        if (getAreaLength() == 0) {
            return;
        }

        breakIterator.setText(area.getText());
        int position = calculatePositionViaBreakingBackwards(numOfBreaks, breakIterator, getStartPosition());
        updateEndTo(position);
    }

    @Override
    public void selectAll() {
        selectRange(0, getAreaLength());
    }

    @Override
    public void selectParagraph(int paragraphIndex) {
        int start = textPosition(paragraphIndex, 0);
        int end = area.getParagraphLength(paragraphIndex);
        selectRange(start, end);
    }

    @Override
    public void selectWord(int wordPositionInArea) {
        if (getAreaLength() == 0) {
            return;
        }

        BreakIterator breakIterator = BreakIterator.getWordInstance();
        breakIterator.setText(area.getText());

        int start = calculatePositionViaBreakingBackwards(1, breakIterator, wordPositionInArea);
        int end = calculatePositionViaBreakingForwards(1, breakIterator, wordPositionInArea);
        selectRange(start, end);
    }

    @Override
    public void deselect() {
        selectRangeExpl(getPosition(), getPosition());
    }

    // caret selection bind
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
    public void moveTo(int pos, NavigationActions.SelectionPolicy selectionPolicy) {
        switch(selectionPolicy) {
            case CLEAR:
                selectRangeExpl(pos, pos);
                break;
            case ADJUST:
                selectRangeExpl(getAnchorPosition(), pos);
                break;
            case EXTEND:
                IndexRange sel = getRange();
                int anchor;
                if (pos <= sel.getStart()) {
                    anchor = sel.getEnd();
                } else if(pos >= sel.getEnd()) {
                    anchor = sel.getStart();
                } else {
                    anchor = getAnchorPosition();
                }
                selectRangeExpl(anchor, pos);
                break;
        }
    }

    @Override
    public void moveTo(int paragraphIndex, int columnIndex, NavigationActions.SelectionPolicy selectionPolicy) {
        moveTo(textPosition(paragraphIndex, columnIndex), selectionPolicy);
    }

    @Override
    public void moveToPrevChar(NavigationActions.SelectionPolicy selectionPolicy) {
        if (getPosition() > 0) {
            int newCaretPos = Character.offsetByCodePoints(area.getText(), getPosition(), -1);
            moveTo(newCaretPos, selectionPolicy);
        }
    }

    @Override
    public void moveToNextChar(NavigationActions.SelectionPolicy selectionPolicy) {
        if (getPosition() < getAreaLength()) {
            int newCaretPos = Character.offsetByCodePoints(area.getText(), getPosition(), 1);
            moveTo(newCaretPos, selectionPolicy);
        }
    }

    @Override
    public void moveToParStart(NavigationActions.SelectionPolicy selectionPolicy) {
        moveTo(getPosition() - getColumnPosition(), selectionPolicy);
    }

    @Override
    public void moveToParEnd(NavigationActions.SelectionPolicy selectionPolicy) {
        int newPos = area.getParagraphLength(getParagraphIndex());
        moveTo(newPos, selectionPolicy);
    }

    @Override
    public void moveToAreaStart(NavigationActions.SelectionPolicy selectionPolicy) {
        moveTo(0, selectionPolicy);
    }

    @Override
    public void moveToAreaEnd(NavigationActions.SelectionPolicy selectionPolicy) {
        moveTo(area.getLength(), selectionPolicy);
    }

    @Override
    public void displaceCaret(int position) {
        doUpdate(() -> delegateCaret.moveTo(position));
    }

    @Override
    public void displaceSelection(int startPosition, int endPosition) {
        doUpdate(() -> {
            delegateSelection.selectRange(startPosition, endPosition);
            internalStartedByAnchor.setValue(startPosition < endPosition);
        });
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
        doUpdate(() -> {
            delegateSelection.selectRange(startPosition, endPosition);
            internalStartedByAnchor.setValue(anchorIsStart);

            delegateCaret.moveTo(anchorIsStart ? endPosition : startPosition);
        });
    }

    private void doUpdate(Runnable updater) {
        if (isBeingUpdated()) {
            updater.run();
        } else {
            area.beingUpdatedProperty().suspendWhile(updater);
        }
    }

    private int textPosition(int paragraphIndex, int columnPosition) {
        return area.position(paragraphIndex, columnPosition).toOffset();
    }

    private int getAreaLength() { return area.getLength(); }

    /** Assumes that {@code area.getLength != 0} is true and {@link BreakIterator#setText(String)} has been called */
    private int calculatePositionViaBreakingForwards(int numOfBreaks, BreakIterator breakIterator, int position) {
        breakIterator.following(position);
        for (int i = 1; i < numOfBreaks; i++) {
            breakIterator.next(numOfBreaks);
        }
        return breakIterator.current();
    }

    /** Assumes that {@code area.getLength != 0} is true and {@link BreakIterator#setText(String)} has been called */
    private int calculatePositionViaBreakingBackwards(int numOfBreaks, BreakIterator breakIterator, int position) {
        breakIterator.preceding(position);
        for (int i = 1; i < numOfBreaks; i++) {
            breakIterator.previous();
        }
        return breakIterator.current();
    }

}
