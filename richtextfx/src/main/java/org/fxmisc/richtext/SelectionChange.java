package org.fxmisc.richtext;

import org.fxmisc.richtext.model.PlainTextChange;

import java.util.List;

class SelectionChange {
    // TODO -> to be replaced by state of this class start(), end()
    public record Range(int start, int end) {}

    public Range apply(List<PlainTextChange> list, int selectStart, int selectEnd) {
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
        return new Range(selectStart, selectEnd);
    }
}
