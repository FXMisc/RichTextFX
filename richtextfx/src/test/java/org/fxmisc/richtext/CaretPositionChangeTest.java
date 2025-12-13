package org.fxmisc.richtext;

import org.fxmisc.richtext.model.PlainTextChange;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class CaretPositionChangeTest {
    private void checkChange(int before, int after, PlainTextChange... changes) {
        assertEquals(after, new CaretPositionChange().apply(before, Arrays.stream(changes).toList()));
    }

    @Test
    public void insertNewText() {
        // After
        checkChange(3, 3, new PlainTextChange(4, "", "and"));
        // On caret
        checkChange(3, 6, new PlainTextChange(3, "", "and"));
        // Before caret (caret within the change range)
        checkChange(3, 2, new PlainTextChange(2, "", "and"));
        // Before caret (caret outside the change range)
        checkChange(4, 7, new PlainTextChange(1, "", "and"));
    }

    @Test
    public void deleteText() {
        // After
        checkChange(3, 3, new PlainTextChange(4, "and", ""));
        // On caret
        checkChange(3, 3, new PlainTextChange(3, "and", ""));
        // Before caret (caret within the change range)
        checkChange(3, 2, new PlainTextChange(2, "and", ""));
        // Before caret (caret outside the change range)
        checkChange(3, 0, new PlainTextChange(0, "and", ""));
        checkChange(4, 1, new PlainTextChange(0, "and", ""));
    }

    @Test
    public void replaceWithSameSize() {
        // After
        checkChange(3, 3, new PlainTextChange(4, "and", "but"));
        // On caret
        checkChange(3, 3, new PlainTextChange(3, "and", "but"));
        // Before caret (caret within the change range)
        checkChange(3, 3, new PlainTextChange(2, "and", "but"));
        // Before caret (caret outside the change range)
        checkChange(5, 5, new PlainTextChange(2, "and", "but"));
    }

    // TODO SMA continue adding tests
}
