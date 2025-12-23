package org.fxmisc.richtext;

import org.fxmisc.richtext.model.PlainTextChange;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class SelectionChangeTest {
    private void checkChange(int startBefore, int endBefore, int startAfter, int endAfter, PlainTextChange... changes) {
        List<PlainTextChange> changeList = Arrays.stream(changes).collect(Collectors.toList());
        SelectionChange.Range range = new SelectionChange().apply(changeList, startBefore, endBefore);
        assertEquals("Start does not match", startAfter, range.start());
        assertEquals("End does not match", endAfter, range.end());
    }

    @Test
    public void noChange() {
        // Moving end closer to start
        checkChange(1, 5, 1, 5, new PlainTextChange(3, "", ""));
        checkChange(1, 4, 1, 4, new PlainTextChange(3, "", ""));
        checkChange(1, 3, 1, 3, new PlainTextChange(3, "", ""));
        checkChange(1, 2, 1, 2, new PlainTextChange(3, "", ""));
        checkChange(1, 1, 1, 1, new PlainTextChange(3, "", ""));

        // Moving start closer to end
        checkChange(1, 5, 1, 5, new PlainTextChange(3, "", ""));
        checkChange(2, 5, 2, 5, new PlainTextChange(3, "", ""));
        checkChange(3, 5, 3, 5, new PlainTextChange(3, "", ""));
        checkChange(4, 5, 4, 5, new PlainTextChange(3, "", ""));
        checkChange(5, 5, 5, 5, new PlainTextChange(3, "", ""));

        // No interval selected
        checkChange(1, 1, 1, 1, new PlainTextChange(3, "", ""));
        checkChange(2, 2, 2, 2, new PlainTextChange(3, "", ""));
        checkChange(3, 3, 3, 3, new PlainTextChange(3, "", ""));
        checkChange(4, 4, 4, 4, new PlainTextChange(3, "", ""));
        checkChange(5, 5, 5, 5, new PlainTextChange(3, "", ""));
    }

    @Test
    public void changeSameSize() {
        // Moving end closer to start
        checkChange(1, 8, 1, 8, new PlainTextChange(3, "One", "Two"));
        checkChange(1, 7, 1, 7, new PlainTextChange(3, "One", "Two"));
        checkChange(1, 6, 1, 6, new PlainTextChange(3, "One", "Two"));
        checkChange(1, 5, 1, 5, new PlainTextChange(3, "One", "Two"));
        checkChange(1, 4, 1, 4, new PlainTextChange(3, "One", "Two"));
        checkChange(1, 3, 1, 3, new PlainTextChange(3, "One", "Two"));
        checkChange(1, 2, 1, 2, new PlainTextChange(3, "One", "Two"));
        checkChange(1, 1, 1, 1, new PlainTextChange(3, "One", "Two"));

        // Moving start closer to end
        checkChange(1, 8, 1, 8, new PlainTextChange(3, "One", "Two"));
        checkChange(2, 8, 2, 8, new PlainTextChange(3, "One", "Two"));
        checkChange(3, 8, 3, 8, new PlainTextChange(3, "One", "Two"));
        checkChange(4, 8, 4, 8, new PlainTextChange(3, "One", "Two"));
        checkChange(5, 8, 5, 8, new PlainTextChange(3, "One", "Two"));
        checkChange(6, 8, 6, 8, new PlainTextChange(3, "One", "Two"));
        checkChange(7, 8, 7, 8, new PlainTextChange(3, "One", "Two"));
        checkChange(8, 8, 8, 8, new PlainTextChange(3, "One", "Two"));

        // No interval selected
        checkChange(1, 1, 1, 1, new PlainTextChange(3, "One", "Two"));
        checkChange(2, 2, 2, 2, new PlainTextChange(3, "One", "Two"));
        checkChange(3, 3, 3, 3, new PlainTextChange(3, "One", "Two"));
        checkChange(4, 4, 4, 4, new PlainTextChange(3, "One", "Two"));
        checkChange(5, 5, 5, 5, new PlainTextChange(3, "One", "Two"));
        checkChange(6, 6, 6, 6, new PlainTextChange(3, "One", "Two"));
        checkChange(7, 7, 7, 7, new PlainTextChange(3, "One", "Two"));
        checkChange(8, 8, 8, 8, new PlainTextChange(3, "One", "Two"));
    }

    @Test
    public void removingContent() {
        // Moving end closer to start
        checkChange(1, 8, 1, 5, new PlainTextChange(3, "One", ""));
        checkChange(1, 7, 1, 4, new PlainTextChange(3, "One", ""));
        checkChange(1, 6, 1, 3, new PlainTextChange(3, "One", ""));
        checkChange(1, 5, 1, 3, new PlainTextChange(3, "One", ""));
        checkChange(1, 4, 1, 3, new PlainTextChange(3, "One", ""));
        checkChange(1, 3, 1, 3, new PlainTextChange(3, "One", ""));
        checkChange(1, 2, 1, 2, new PlainTextChange(3, "One", ""));
        checkChange(1, 1, 1, 1, new PlainTextChange(3, "One", ""));

        // Moving start closer to end
        checkChange(1, 8, 1, 5, new PlainTextChange(3, "One", ""));
        checkChange(2, 8, 2, 5, new PlainTextChange(3, "One", ""));
        checkChange(3, 8, 3, 5, new PlainTextChange(3, "One", ""));
        checkChange(4, 8, 3, 5, new PlainTextChange(3, "One", ""));
        checkChange(5, 8, 3, 5, new PlainTextChange(3, "One", ""));
        checkChange(6, 8, 3, 5, new PlainTextChange(3, "One", ""));
        checkChange(7, 8, 4, 5, new PlainTextChange(3, "One", ""));
        checkChange(8, 8, 5, 5, new PlainTextChange(3, "One", ""));

        // No interval selected
        checkChange(1, 1, 1, 1, new PlainTextChange(3, "One", ""));
        checkChange(2, 2, 2, 2, new PlainTextChange(3, "One", ""));
        checkChange(3, 3, 3, 3, new PlainTextChange(3, "One", ""));
        checkChange(4, 4, 3, 3, new PlainTextChange(3, "One", ""));
        checkChange(5, 5, 3, 3, new PlainTextChange(3, "One", ""));
        checkChange(6, 6, 3, 3, new PlainTextChange(3, "One", ""));
        checkChange(7, 7, 4, 4, new PlainTextChange(3, "One", ""));
        checkChange(8, 8, 5, 5, new PlainTextChange(3, "One", ""));
    }

    @Test
    public void addingContent() {
        // Moving end closer to start
        checkChange(1, 5, 1, 3, new PlainTextChange(3, "", "Two"));
        checkChange(1, 4, 1, 3, new PlainTextChange(3, "", "Two"));
        checkChange(1, 3, 1, 3, new PlainTextChange(3, "", "Two"));
        checkChange(1, 2, 1, 2, new PlainTextChange(3, "", "Two"));
        checkChange(1, 1, 1, 1, new PlainTextChange(3, "", "Two"));

        // Moving start closer to end
        checkChange(1, 5, 1, 3, new PlainTextChange(3, "", "Two"));
        checkChange(2, 5, 2, 3, new PlainTextChange(3, "", "Two"));
        checkChange(3, 5, 3, 3, new PlainTextChange(3, "", "Two"));
        checkChange(4, 5, 3, 3, new PlainTextChange(3, "", "Two"));
        checkChange(5, 5, 3, 3, new PlainTextChange(3, "", "Two"));

        // No interval selected
        checkChange(1, 1, 1, 1, new PlainTextChange(3, "", "Two"));
        checkChange(2, 2, 2, 2, new PlainTextChange(3, "", "Two"));
        checkChange(3, 3, 3, 3, new PlainTextChange(3, "", "Two"));
        // Bug: If you are at position 4 you need to move forward by three
        checkChange(4, 4, 3, 3, new PlainTextChange(3, "", "Two"));
        checkChange(5, 5, 3, 3, new PlainTextChange(3, "", "Two"));
    }

    @Test
    public void reduceSize() {
        // Moving end closer to start
        checkChange(1, 8, 1, 6, new PlainTextChange(3, "One", "A"));
        checkChange(1, 7, 1, 5, new PlainTextChange(3, "One", "A"));
        checkChange(1, 6, 1, 4, new PlainTextChange(3, "One", "A"));
        checkChange(1, 5, 1, 3, new PlainTextChange(3, "One", "A"));
        checkChange(1, 4, 1, 3, new PlainTextChange(3, "One", "A"));
        checkChange(1, 3, 1, 3, new PlainTextChange(3, "One", "A"));
        checkChange(1, 2, 1, 2, new PlainTextChange(3, "One", "A"));
        checkChange(1, 1, 1, 1, new PlainTextChange(3, "One", "A"));

        // Moving start closer to end
        checkChange(1, 8, 1, 6, new PlainTextChange(3, "One", "A"));
        checkChange(2, 8, 2, 6, new PlainTextChange(3, "One", "A"));
        checkChange(3, 8, 3, 6, new PlainTextChange(3, "One", "A"));
        checkChange(4, 8, 3, 6, new PlainTextChange(3, "One", "A"));
        checkChange(5, 8, 3, 6, new PlainTextChange(3, "One", "A"));
        checkChange(6, 8, 4, 6, new PlainTextChange(3, "One", "A"));
        checkChange(7, 8, 5, 6, new PlainTextChange(3, "One", "A"));
        checkChange(8, 8, 6, 6, new PlainTextChange(3, "One", "A"));

        // No interval selected
        checkChange(1, 1, 1, 1, new PlainTextChange(3, "One", "A"));
        checkChange(2, 2, 2, 2, new PlainTextChange(3, "One", "A"));
        checkChange(3, 3, 3, 3, new PlainTextChange(3, "One", "A"));
        checkChange(4, 4, 3, 3, new PlainTextChange(3, "One", "A"));
        checkChange(5, 5, 3, 3, new PlainTextChange(3, "One", "A"));
        checkChange(6, 6, 4, 4, new PlainTextChange(3, "One", "A"));
        checkChange(7, 7, 5, 5, new PlainTextChange(3, "One", "A"));
        checkChange(8, 8, 6, 6, new PlainTextChange(3, "One", "A"));
    }

    @Test
    public void increaseSize() {
        // Moving end closer to start
        checkChange(1, 8, 1, 9, new PlainTextChange(3, "One", "Four"));
        checkChange(1, 7, 1, 8, new PlainTextChange(3, "One", "Four"));
        checkChange(1, 6, 1, 7, new PlainTextChange(3, "One", "Four"));
        checkChange(1, 5, 1, 6, new PlainTextChange(3, "One", "Four"));
        checkChange(1, 4, 1, 5, new PlainTextChange(3, "One", "Four"));
        checkChange(1, 3, 1, 3, new PlainTextChange(3, "One", "Four"));
        checkChange(1, 2, 1, 2, new PlainTextChange(3, "One", "Four"));
        checkChange(1, 1, 1, 1, new PlainTextChange(3, "One", "Four"));

        // Moving start closer to end
        checkChange(1, 8, 1, 9, new PlainTextChange(3, "One", "Four"));
        checkChange(2, 8, 2, 9, new PlainTextChange(3, "One", "Four"));
        checkChange(3, 8, 4, 9, new PlainTextChange(3, "One", "Four"));
        checkChange(4, 8, 5, 9, new PlainTextChange(3, "One", "Four"));
        checkChange(5, 8, 6, 9, new PlainTextChange(3, "One", "Four"));
        checkChange(6, 8, 7, 9, new PlainTextChange(3, "One", "Four"));
        checkChange(7, 8, 8, 9, new PlainTextChange(3, "One", "Four"));
        checkChange(8, 8, 9, 9, new PlainTextChange(3, "One", "Four"));

        // No interval
        checkChange(8, 8, 10, 10, new PlainTextChange(4, "and", "above"));
        checkChange(7, 7, 9, 9, new PlainTextChange(4, "and", "above"));
        checkChange(6, 6, 8, 8, new PlainTextChange(4, "and", "above"));
        // Below is rather strange, why would you suddenly move at the start if you were inside, but not if you were at position 6?
        checkChange(5, 5, 4, 4, new PlainTextChange(4, "and", "above"));
        checkChange(4, 4, 4, 4, new PlainTextChange(4, "and", "above"));
        checkChange(3, 3, 3, 3, new PlainTextChange(4, "and", "above"));
        checkChange(2, 2, 2, 2, new PlainTextChange(4, "and", "above"));
        checkChange(1, 1, 1, 1, new PlainTextChange(4, "and", "above"));
    }

    @Test
    public void multipleChanges() {
        checkChange(1, 8, 0, 0,
                new PlainTextChange(3, "One", "Four"), // 1,8 -> 1,9
                new PlainTextChange(2, "This", ""), // 1,9 -> 1,5
                new PlainTextChange(3, "A", "Test"), // 1,5 -> 1,3
                new PlainTextChange(0, "", "Test") // 1,3 -> 5,8
        );
    }
}
