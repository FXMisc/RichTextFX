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

    private void applyFor(PlainTextChange plainTextChange) {
        int netLength = plainTextChange.getNetLength();
        if (netLength != 0) {
            int changeStart = plainTextChange.getPosition();
            int changeEnd = changeStart + Math.abs(netLength);
            position = applyChange(position, changeStart, changeEnd, netLength);
        }
    }

    /**
     * <ul>
     * <li>If the position is after the interval of the change, the caret moves left/right if it's a net removal/addition.</li>
     * <li>If it's within the change interval, the caret move back at the start of the change.</li>
     * <li>If it's equal to the start position, the caret is moved at the end of the changed (it cannot be moved backwards
     * if it's a removal of text, hence capping to 0).</li>
     * </ul>
     */
    private static int applyChange(int position, int changeStart, int changeEnd, int netLength) {
        if(position >= changeEnd) {
            position += netLength;
        }
        else if(position > changeStart) {
            position = changeStart;
        }
        else if(position == changeStart) {
            position += Math.max(0, netLength);
        }
        return position;
    }
}
