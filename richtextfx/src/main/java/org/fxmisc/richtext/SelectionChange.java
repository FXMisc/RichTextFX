package org.fxmisc.richtext;

import org.fxmisc.richtext.model.PlainTextChange;

import java.util.List;

// TODO SMA can it be that CaretPositionChange is the same as this one but for start == end ?
class SelectionChange {
    private int start, end;

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
        this.start = start;
        this.end = end;
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
        return new Range(start, end);
    }

    private void applyFor(PlainTextChange plainTextChange) {
        int netLength = plainTextChange.getNetLength();
        int changeStart = plainTextChange.getPosition();
        // in case of a replacement: "hello there" -> "hi."
        int changeEnd = changeStart + Math.abs(netLength);
        if (start == changeStart) {
            start += Math.max(netLength, 0);
        }
        else {
            start = applyChange(start, changeStart, changeEnd, netLength);
        }
        end = applyChange(end, changeStart, changeEnd, netLength);
        start = Math.min(start, end);
    }

    private static int applyChange(int position, int changeStart, int changeEnd, int netLength) {
        if(position >= changeEnd) {
            position += netLength;
        }
        else if(position > changeStart) {
            position = changeStart;
        }
        return position;
    }
}
