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

    public Range applyFor(List<PlainTextChange> changes) {
        changes.forEach(this::applyFor);
        return new Range(start, end);
    }

    private void applyFor(PlainTextChange plainTextChange) {
        int netLength = plainTextChange.getNetLength();
        int changeStart = plainTextChange.getPosition();
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
