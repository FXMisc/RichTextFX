package org.fxmisc.richtext;

import org.fxmisc.richtext.model.PlainTextChange;

import java.util.List;

class CaretPositionChange {
    private int caretPosition;

    public CaretPositionChange(int caretPosition) {
        this.caretPosition = caretPosition;
    }

    /**
     * Goes through the list of change and update the caret position accordingly
     * @param list the list of changes
     * @return the new caret position
     */
    public int apply(List<PlainTextChange> list) {
        list.forEach(this::apply);
        return caretPosition;
    }

    private void apply(PlainTextChange plainTextChange) {
        int netLength = plainTextChange.getNetLength();
        if (netLength != 0) {
            int changePosition = plainTextChange.getPosition();
            if (caretPosition == changePosition) {
                // If it's net removal, we don't change anything
                caretPosition += Math.max(0, netLength);
            }
            else if (caretPosition > changePosition) {
                // If the caret is within the change, we set it to the start of the change
                if(caretPosition < changePosition + Math.abs(netLength)) {
                    caretPosition = changePosition;
                }
                // Else we move the caret depending on the length variation
                else {
                    caretPosition += netLength;
                }
            }
            // If caretPosition < changeIndex, there is no movement as the changes happens after the current position
        }
    }
}
