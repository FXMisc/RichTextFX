package org.fxmisc.richtext;

import org.fxmisc.richtext.model.PlainTextChange;

import java.util.List;

// TODO SMA can it be that CaretPositionChange is the same as this one but for start == end ?
class SelectionChange {
    private Range range;

    // TODO -> to be replaced by state of this class start(), end()
    public static class Range {
        private final int start, end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int start() {
            return start;
        }

        public int end() {
            return end;
        }
    }

    public SelectionChange(int start, int end) {
        this.range = new Range(start, end);
    }

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
    public Range applyFor(List<PlainTextChange> changes) {
        changes.forEach(this::applyFor);
        return range;
    }

    private void applyFor(PlainTextChange plainTextChange) {
        int start = range.start();
        int end = range.end();

        int netLength = plainTextChange.getNetLength();
        int indexOfChange = plainTextChange.getPosition();
        // in case of a replacement: "hello there" -> "hi."
        int endOfChange = indexOfChange + Math.abs(netLength);

        start = evaluatePosition(indexOfChange, netLength, endOfChange, start);

        if (indexOfChange < end) {
            end = end < endOfChange ? indexOfChange : end + netLength;
        }
        start = Math.min(start, end);
        this.range = new Range(start, end);
    }

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
