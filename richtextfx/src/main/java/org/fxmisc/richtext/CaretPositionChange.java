package org.fxmisc.richtext;

import org.fxmisc.richtext.model.PlainTextChange;

import java.util.List;

class CaretPositionChange {
    /**
     * Goes through the list of change and update the caret position accordingly
     * @param position the current caret position
     * @param list the list of changes
     * @return the new caret position
     */
    public int apply(int position, List<PlainTextChange> list) {
        for (PlainTextChange plainTextChange : list) {
            int netLength = plainTextChange.getNetLength();
            if (netLength != 0) {
                int indexOfChange = plainTextChange.getPosition();
                // in case of a replacement: "hello there" -> "hi."
                int endOfChange = indexOfChange + Math.abs(netLength);

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
                if (indexOfChange == position && netLength > 0) {
                    position = position + netLength;
                } else if (indexOfChange < position) {
                    position = position < endOfChange ? indexOfChange : position + netLength;
                }
            }
        }
        return position;
    }
}
