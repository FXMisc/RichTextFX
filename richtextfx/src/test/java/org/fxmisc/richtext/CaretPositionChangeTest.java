package org.fxmisc.richtext;

import org.fxmisc.richtext.model.PlainTextChange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class CaretPositionChangeTest {
    private void checkChange(int before, int after, PlainTextChange... changes) {
        List<PlainTextChange> changeList = Arrays.stream(changes).collect(Collectors.toList());
        assertEquals(after, new CaretPositionChange(before).apply(changeList));
    }

    @ParameterizedTest
    @MethodSource("argNoChange")
    @DisplayName("Caret position does not change when no text change")
    public void noChange(int before, int after) {
        checkChange(before, after, new PlainTextChange(4, "", ""));
    }

    private static Stream<Arguments> argNoChange() {
        return Stream.of(Arguments.of(3, 3), Arguments.of(4, 4), Arguments.of(5, 5));
    }

    @ParameterizedTest
    @MethodSource("argsInsertNewText")
    @DisplayName("Insert new text at given position")
    void insertNewText(int before, int after) {
        checkChange(before, after, new PlainTextChange(4, "", "and"));
    }

    private static Stream<Arguments> argsInsertNewText() {
        return Stream.of(
                Arguments.of(3, 3), Arguments.of(4, 7),
                Arguments.of(5, 4), Arguments.of(6, 4),
                Arguments.of(7, 10), Arguments.of(8, 11));
    }

    @ParameterizedTest
    @MethodSource("argsDeleteText")
    @DisplayName("Delete text at a given position")
    public void deleteText(int before, int after) {
        checkChange(before, after, new PlainTextChange(4, "and", ""));
    }

    private static Stream<Arguments> argsDeleteText() {
        return Stream.of(
                Arguments.of(3, 3), Arguments.of(4, 4), Arguments.of(5, 4),
                Arguments.of(6, 4), Arguments.of(7, 4), Arguments.of(8, 5),
                Arguments.of(9, 6));
    }

    @ParameterizedTest
    @MethodSource("argsReplaceWithSameSize")
    @DisplayName("Replace text with content of same size")
    public void replaceWithSameSize(int before, int after) {
        checkChange(before, after, new PlainTextChange(4, "and", "but"));
    }

    private static Stream<Arguments> argsReplaceWithSameSize() {
        return Stream.of(
                Arguments.of(3, 3), Arguments.of(4, 4), Arguments.of(5, 5),
                Arguments.of(6, 6), Arguments.of(7, 7), Arguments.of(8, 8));
    }

    @ParameterizedTest
    @MethodSource("argsReplaceWithGreaterSize")
    @DisplayName("Replace text with content of greater size")
    public void replaceWithGreaterSize(int before, int after) {
        checkChange(before, after, new PlainTextChange(4, "and", "above"));
    }

    private static Stream<Arguments> argsReplaceWithGreaterSize() {
        return Stream.of(
                Arguments.of(3, 3), Arguments.of(4, 6), Arguments.of(5, 4),
                // This one below is odd. If you have "Two and one" with caret at position 6, the "and" is replaced
                // by "above" to form "Two above one", the caret moves to position 8. I couldn't find a bug when ran
                // manually, as this code does not seem to be used for that update (which might indicate duplicate
                // code but that is a different problem).
                // I'm suspecting a bug.
                Arguments.of(6, 8),
                Arguments.of(7, 9), Arguments.of(8, 10),
                Arguments.of(9, 11), Arguments.of(10, 12));
    }

    @ParameterizedTest
    @MethodSource("argsReplaceWithSmallerSize")
    @DisplayName("Replace text with content of smaller size")
    public void replaceWithSmallerSize(int before, int after) {
        checkChange(before, after, new PlainTextChange(4, "hover", "and"));
    }

    private static Stream<Arguments> argsReplaceWithSmallerSize() {
        return Stream.of(
                Arguments.of(3, 3), Arguments.of(4, 4), Arguments.of(5, 4),
                Arguments.of(6, 4), Arguments.of(7, 5), Arguments.of(8, 6),
                Arguments.of(9, 7), Arguments.of(10, 8));
    }

    @Test
    @DisplayName("Observe caret change after multiple changes")
    public void multipleChangesOfAllFlavours() {
        checkChange(4, 1,
                new PlainTextChange(4, "and", "above"), // 4 -> 6
                new PlainTextChange(4, "above", "below"), // 6 -> 6
                new PlainTextChange(4, "below", "bell"), // 6 -> 5
                new PlainTextChange(1, "cowbell", "") // 5 -> 1
        );
    }
}
