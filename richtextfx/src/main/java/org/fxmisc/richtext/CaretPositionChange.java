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
            int changePosition = plainTextChange.getPosition();
            if (position == changePosition) {
                // If it's net removal, we don't change anything
                position += Math.max(0, netLength);
            }
            else if (position > changePosition) {
                // If the caret is within the change, we set it to the start of the change
                if(position < changePosition + Math.abs(netLength)) {
                    position = changePosition;
                }
                // Else we move the caret depending on the length variation
                else {
                    position += netLength;
                }
            }
            // If caretPosition < changeIndex, there is no movement as the changes happens after the current position
        }
    }
}
