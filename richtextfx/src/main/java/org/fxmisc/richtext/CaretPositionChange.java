package org.fxmisc.richtext;

import org.fxmisc.richtext.model.PlainTextChange;

import java.util.List;

class CaretPositionChange {
    private int position;

    /** @param position the current caret position */
    public CaretPositionChange(int position) {
        this.position = position;
    }

    /**
     * Goes through the list of change and update the caret position accordingly
     * @return the new caret position
     */
    public int apply(List<PlainTextChange> changes) {
        changes.forEach(this::applyFor);
        return position;
    }


    /*
        "->" means add (positive) netLength to position
        "<-" means add (negative) netLength to position
        "x" means don't update position

        "+c" means caret was included in the deleted portion of content
        "-c" means caret was not included in the deleted portion of content
        Before/At/After means indexOfChange "<" / "==" / ">" position

               |   Before +c   | Before -c | At | After
        -------+---------------+-----------+----+------
        Add    |      N/A      |    ->     | -> | x
        Delete | indexOfChange |    <-     | x  | x
     */
    private void applyFor(PlainTextChange plainTextChange) {
        int netLength = plainTextChange.getNetLength();
        if (netLength != 0) {
            int indexOfChange = plainTextChange.getPosition();
            // in case of a replacement: "hello there" -> "hi."
            int endOfChange = indexOfChange + Math.abs(netLength);
            position = evaluatePosition(indexOfChange, netLength, endOfChange, position);
        }
    }

    // TODO SMA duplicate with selection change -> it seems the code is simply
    private int evaluatePosition(int indexOfChange, int netLength, int endOfChange, int position) {
        if (indexOfChange == position && netLength > 0) {
            position = position + netLength;
        }
        else if (indexOfChange < position) {
            position = position < endOfChange ? indexOfChange : position + netLength;
        }
        return position;
    }
}
