package org.fxmisc.richtext.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.fxmisc.richtext.model.StyleSpansChecker.checkStyle;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StyleSpansBuilderTest {
    @Test
    @DisplayName("Cannot create an empty style span")
    void empty() {
        assertThrows(IllegalStateException.class, () -> new StyleSpansBuilder<>().create());
    }

    @Test
    @DisplayName("Creating a single-style style span")
    void createStyleSpan() {
        StyleSpans<String> style = new StyleSpansBuilder<String>().add("alpha", 10).create();
        checkStyle(style, 10, new String[] {"alpha"}, 0, 10);
    }

    @Test
    @DisplayName("Adding only empty styles")
    void onlyEmptyStyles() {
        StyleSpans<String> style = new StyleSpansBuilder<String>()
                .add("alpha", 0)
                .add("beta", 0)
                .add("charlie", 0)
                .create();
        checkStyle(style, 0, new String[] {"alpha"}, 0, 0);
    }

    @Test
    @DisplayName("Adding style on only empty, overwrites it")
    void nonEmptyOnEmpty() {
        StyleSpans<String> style = new StyleSpansBuilder<String>()
                .add("alpha", 0)
                .add("beta", 0)
                .add("charlie", 0)
                .add("delta", 1)
                .create();
        checkStyle(style, 1, new String[] {"delta"}, 0, 1);
    }

    @Test
    @DisplayName("Add a list of only one style will merge them all")
    void addListOfOneStyle() {
        StyleSpans<String> style = new StyleSpansBuilder<String>()
                .addAll(List.of(new StyleSpan<>("alpha", 1), new StyleSpan<>("alpha", 2), new StyleSpan<>("alpha", 3)))
                .create();
        checkStyle(style, 6, new String[] {"alpha"}, 0, 6);
    }

    @Test
    @DisplayName("Creating a multi-style style span")
    void multiStyleSpan() {
        StyleSpans<String> style = new StyleSpansBuilder<String>()
                .add("alpha", 1)
                .add("alpha", 2)
                .add("beta", 3)
                .add("delta", 0)
                .add("charlie", 4)
                .add("alpha", 5)
                .create();
        checkStyle(style, 15, new String[] {"alpha", "beta", "charlie", "alpha"}, 0, 3, 3, 6, 6, 10, 10, 15);
    }
}
