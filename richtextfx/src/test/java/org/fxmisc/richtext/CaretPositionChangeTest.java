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
    public void noChange() {
        checkChange(3, 3, new PlainTextChange(4, "", ""));
        checkChange(4, 4, new PlainTextChange(4, "", ""));
        checkChange(5, 5, new PlainTextChange(4, "", ""));
    }

    @Test
    public void insertNewText() {
        checkChange(3, 3, new PlainTextChange(4, "", "and"));
        checkChange(4, 7, new PlainTextChange(4, "", "and"));
        checkChange(5, 4, new PlainTextChange(4, "", "and"));
        checkChange(6, 4, new PlainTextChange(4, "", "and"));
        checkChange(7, 10, new PlainTextChange(4, "", "and"));
        checkChange(8, 11, new PlainTextChange(4, "", "and"));
    }

    @Test
    public void deleteText() {
        checkChange(3, 3, new PlainTextChange(4, "and", ""));
        checkChange(4, 4, new PlainTextChange(4, "and", ""));
        checkChange(5, 4, new PlainTextChange(4, "and", ""));
        checkChange(6, 4, new PlainTextChange(4, "and", ""));
        checkChange(7, 4, new PlainTextChange(4, "and", ""));
        checkChange(8, 5, new PlainTextChange(4, "and", ""));
        checkChange(9, 6, new PlainTextChange(4, "and", ""));
    }

    @Test
    public void replaceWithSameSize() {
        checkChange(3, 3, new PlainTextChange(4, "and", "but"));
        checkChange(4, 4, new PlainTextChange(4, "and", "but"));
        checkChange(5, 5, new PlainTextChange(4, "and", "but"));
        checkChange(6, 6, new PlainTextChange(4, "and", "but"));
        checkChange(7, 7, new PlainTextChange(4, "and", "but"));
        checkChange(8, 8, new PlainTextChange(4, "and", "but"));
    }

    @Test
    public void replaceWithGreaterSize() {
        checkChange(3, 3, new PlainTextChange(4, "and", "above"));
        checkChange(4, 6, new PlainTextChange(4, "and", "above"));
        checkChange(5, 4, new PlainTextChange(4, "and", "above"));
        checkChange(6, 8, new PlainTextChange(4, "and", "above"));
        checkChange(7, 9, new PlainTextChange(4, "and", "above"));
        checkChange(8, 10, new PlainTextChange(4, "and", "above"));
        checkChange(9, 11, new PlainTextChange(4, "and", "above"));
        checkChange(10, 12, new PlainTextChange(4, "and", "above"));
    }

    @Test
    public void replaceWithSmallerSize() {
        checkChange(3, 3, new PlainTextChange(4, "hover", "and"));
        checkChange(4, 4, new PlainTextChange(4, "hover", "and"));
        checkChange(5, 4, new PlainTextChange(4, "hover", "and"));
        checkChange(6, 4, new PlainTextChange(4, "hover", "and"));
        checkChange(7, 5, new PlainTextChange(4, "hover", "and"));
        checkChange(8, 6, new PlainTextChange(4, "hover", "and"));
        checkChange(9, 7, new PlainTextChange(4, "hover", "and"));
        checkChange(10, 8, new PlainTextChange(4, "hover", "and"));
    }

    @Test
    public void multipleChangesOfAllFlavours() {
        checkChange(4, 1,
                new PlainTextChange(4, "and", "above"), // 4 -> 6
                new PlainTextChange(4, "above", "below"), // 6 -> 6
                new PlainTextChange(4, "below", "bell"), // 6 -> 5
                new PlainTextChange(1, "cowbell", "") // 5 -> 1
        );
    }
}
