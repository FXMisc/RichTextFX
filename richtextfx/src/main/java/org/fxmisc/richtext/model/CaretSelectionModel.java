package org.fxmisc.richtext.model;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.richtext.model.TwoDimensional.Position;
import org.reactfx.EventStream;
import org.reactfx.Guard;
import org.reactfx.Subscription;
import org.reactfx.Suspendable;
import org.reactfx.value.SuspendableVal;
import org.reactfx.value.SuspendableVar;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import static org.fxmisc.richtext.util.Utilities.EMPTY_RANGE;
import static org.fxmisc.richtext.util.Utilities.clamp;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.Backward;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.Forward;
import static org.reactfx.EventStreams.merge;
import static org.reactfx.EventStreams.invalidationsOf;

/**
 * Encapsulates all caret and selection related information for {@link StyledTextAreaModel} and utilizes
 * {@link Suspendable suspendable properties} to insure that changes to these values don't propagate until
 * changes have been finished to the underlying {@link EditableStyledDocument}.
 *
 * <h3>Terms</h3>
 * <ul>
 *     <li>Position: the position in-between letters of a text (e.g. if "|" represents the caret and our text is
 *     "text", then all possible positions are "|t|e|x|t|"</li>
 * </ul>
 *
 */
public final class CaretSelectionModel {

    // caret position
    private final Var<Integer> internalCaretPosition = Var.newSimpleVar(0);
    /**
     * The position of the caret within the text
     */
    private final SuspendableVal<Integer> caretPosition = internalCaretPosition.suspendable();
    public final int getCaretPosition() { return caretPosition.getValue(); }
    public final ObservableValue<Integer> caretPositionProperty() { return caretPosition; }

    // selection anchor
    private final SuspendableVar<Integer> anchor = Var.newSimpleVar(0).suspendable();
    public final int getAnchor() { return anchor.getValue(); }
    public final ObservableValue<Integer> anchorProperty() { return anchor; }

    // selection
    private final Var<IndexRange> internalSelection = Var.newSimpleVar(EMPTY_RANGE);
    private final SuspendableVal<IndexRange> selection = internalSelection.suspendable();
    public final IndexRange getSelection() { return selection.getValue(); }
    public final ObservableValue<IndexRange> selectionProperty() { return selection; }

    // selected text
    private final SuspendableVal<String> selectedText;
    public final String getSelectedText() { return selectedText.getValue(); }
    public final ObservableValue<String> selectedTextProperty() { return selectedText; }

    // caret paragraph index
    private final SuspendableVal<Integer> caretParagraph;
    public final int getCaretParagraph() { return caretParagraph.getValue(); }
    public final ObservableValue<Integer> caretParagraphProperty() { return caretParagraph; }

    // caret column position
    private final SuspendableVal<Integer> caretColumn;
    public final int getCaretColumn() { return caretColumn.getValue(); }
    public final ObservableValue<Integer> caretColumnProperty() { return caretColumn; }

    private final EventStream<?> caretDirty;
    public final EventStream<?> caretDirtyEvents() { return caretDirty; }

    private final EventStream<?> selectionDirty;
    public final EventStream<?> selectionDirtyEvents() { return selectionDirty; }

    private Position selectionStart2D;
    private Position selectionEnd2D;

    private final StyledTextAreaModel<?, ?, ?> model;
    private final Suspendable modelBeingUpdated;
    private final Subscription subscription;

    CaretSelectionModel(StyledTextAreaModel<?, ?, ?> model, Suspendable modelBeingUpdated) {
        this.model = model;
        this.modelBeingUpdated = modelBeingUpdated;
        Val<Position> caretPosition2D = Val.create(
                () -> offsetToPosition(internalCaretPosition.getValue(), Forward),
                internalCaretPosition, model.getParagraphs()
        );
        caretParagraph = caretPosition2D.map(Position::getMajor).suspendable();
        caretColumn = caretPosition2D.map(Position::getMinor).suspendable();

        selectionStart2D = position(0, 0);
        selectionEnd2D = position(0, 0);
        internalSelection.addListener(obs -> {
            IndexRange sel = internalSelection.getValue();
            selectionStart2D = offsetToPosition(sel.getStart(), Forward);
            selectionEnd2D = sel.getLength() == 0
                    ? selectionStart2D
                    : selectionStart2D.offsetBy(sel.getLength(), Backward);
        });

        selectedText = Val.create(
                () -> model.getText(internalSelection.getValue()),
                internalSelection, model.getParagraphs()).suspendable();

        // when content is updated by an area, update the caret
        // and selection ranges of all the other
        // clones that also share this document
        subscription = model.plainTextChanges().subscribe(plainTextChange -> {
            int changeLength = plainTextChange.getInserted().length() - plainTextChange.getRemoved().length();
            if (changeLength != 0) {
                int indexOfChange = plainTextChange.getPosition();
                // in case of a replacement: "hello there" -> "hi."
                int endOfChange = indexOfChange + Math.abs(changeLength);

                // update caret
                int caretPosition = getCaretPosition();
                if (indexOfChange < caretPosition) {
                    // if caret is within the changed content, move it to indexOfChange
                    // otherwise offset it by changeLength
                    positionCaret(
                            caretPosition < endOfChange
                                    ? indexOfChange
                                    : caretPosition + changeLength
                    );
                }
                // update selection
                int selectionStart = getSelection().getStart();
                int selectionEnd = getSelection().getEnd();
                if (selectionStart != selectionEnd) {
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
                    selectRange(selectionStart, selectionEnd);
                } else {
                    // force-update internalSelection in case caret is
                    // at the end of area and a character was deleted
                    // (prevents a StringIndexOutOfBoundsException because
                    // selection's end is one char farther than area's length).
                    int internalCaretPos = internalCaretPosition.getValue();
                    selectRange(internalCaretPos, internalCaretPos);
                }
            }
        });

        caretDirty = merge(
                // follow the caret every time the caret position or paragraphs change
                invalidationsOf(caretPositionProperty()),
                invalidationsOf(model.getParagraphs()),
                // need to reposition popup even when caret hasn't moved, but selection has changed (been deselected)
                invalidationsOf(selectionProperty())
        );
        selectionDirty = invalidationsOf(selectionProperty());
    }

    Suspendable omniSuspendable() {
        return Suspendable.combine(caretPosition, anchor, selection, selectedText, caretParagraph, caretColumn);
    }

    Position position(int row, int col) {
        return model.position(row, col);
    }

    Position offsetToPosition(int charOffset, Bias bias) {
        return model.offsetToPosition(charOffset, bias);
    }

    /**
     * Returns the selection range in the given paragraph.
     */
    public IndexRange getParagraphSelection(int paragraph) {
        int startPar = selectionStart2D.getMajor();
        int endPar = selectionEnd2D.getMajor();

        if(paragraph < startPar || paragraph > endPar) {
            return EMPTY_RANGE;
        }

        int start = paragraph == startPar ? selectionStart2D.getMinor() : 0;
        int end = paragraph == endPar ? selectionEnd2D.getMinor() : model.getParagraph(paragraph).length();

        // force selectionProperty() to be valid
        getSelection();

        return new IndexRange(start, end);
    }

    public void selectRange(int anchor, int caretPosition) {
        try(Guard g = suspend(
                this.caretPosition, caretParagraph,
                caretColumn, this.anchor,
                selection, selectedText)) {
            this.internalCaretPosition.setValue(clamp(0, caretPosition, model.getLength()));
            this.anchor.setValue(clamp(0, anchor, model.getLength()));
            this.internalSelection.setValue(IndexRange.normalize(getAnchor(), getCaretPosition()));
        }
    }

    /**
     * Positions only the caret. Doesn't move the anchor and doesn't change
     * the selection. Can be used to achieve the special case of positioning
     * the caret outside or inside the selection, as opposed to always being
     * at the boundary. Use with care.
     */
    public void positionCaret(int pos) {
        try(Guard g = suspend(caretPosition, caretParagraph, caretColumn)) {
            internalCaretPosition.setValue(pos);
        }
    }

    protected void dispose() {
        subscription.unsubscribe();
    }

    private Guard suspend(Suspendable... suspendables) {
        return Suspendable.combine(modelBeingUpdated, Suspendable.combine(suspendables)).suspend();
    }

}
