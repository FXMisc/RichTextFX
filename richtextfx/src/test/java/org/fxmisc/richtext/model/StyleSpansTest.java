package org.fxmisc.richtext.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.fxmisc.richtext.model.StyleSpansChecker.checkStyle;

public class StyleSpansTest {
    private StyleSpans<String> create(String style, int len) {
        StyleSpansBuilder<String> builder = new StyleSpansBuilder<>();
        builder.add(style, len);
        return builder.create();
    }

    @Nested
    @DisplayName("Extract subview")
    class SubViewTest {
        @Test
        @DisplayName("Extract subview from a single style span should return the same style")
        void createSubViewFromSingleStyle() {
            StyleSpans<String> subStyle = create("text", 10).subView(2, 8);
            checkStyle(subStyle, 6, new String[] {"text"}, 0, 6);
        }

        @Test
        @DisplayName("Extract subview cutting in different styles")
        void createSubViewFromMultiple() {
            StyleSpansBuilder<String> builder = new StyleSpansBuilder<>();
            builder.add("alpha", 6);
            builder.add("beta", 7);
            builder.add("charlie", 8);
            StyleSpans<String> styleSpans = builder.create();
            checkStyle(styleSpans, 21, new String[] {"alpha", "beta", "charlie"}, 0, 6, 6, 13, 13, 21);
            checkStyle(styleSpans.subView(6, 13), 7, new String[] {"beta"}, 0, 7);
            // Empty
            checkStyle(styleSpans.subView(7, 7), 0, new String[] {"beta"}, 0, 0); // Strange, why isn't it empty?
            // Inverted indexes
            checkStyle(styleSpans.subView(14, 5), 0, new String[] {"charlie"}, 0, 0);
            // Out of bound
            checkStyle(styleSpans.subView(-1,22), 23, new String[] {"alpha", "beta", "charlie"}, 0, 6, 6, 13, 0, 10);
            // Bug
            checkStyle(styleSpans.subView(6, 14), 8, new String[] {"beta", "charlie"}, 0, 7, 0, 1);
            checkStyle(styleSpans.subView(5, 13), 8, new String[] {"alpha", "beta"}, 0, 1, 0, 7);
            checkStyle(styleSpans.subView(5, 14), 9, new String[] {"alpha", "beta", "charlie"}, 0, 1, 6, 13, 0, 1);
        }
    }

    @Nested
    @DisplayName("Append")
    class AppendTest {
        private StyleSpans<String> base;

        @BeforeEach
        void setup() {
            base = create("text", 10);
            checkStyle(base, 10, new String[]{"text"}, 0, 10);
        }

        @Test
        @DisplayName("Append empty style at the end of an existing style does not do anything")
        void appendEmpty() {
            checkStyle(base.append("text", 0), 10,
                    new String[]{"text"}, 0, 10);

        }

        @Test
        @DisplayName("Append on an empty style will create a new style with the provided values")
        void appendOnEmpty() {
            checkStyle(create("test", 0).append("text", 2), 2,
                    new String[]{"text"}, 0, 2);
        }

        @Test
        @DisplayName("Append a different style to an existing style span should add it at the end")
        void appendDifferentStyle() {
            checkStyle(base.append("else", 2), 12,
                    new String[]{"text", "else"}, 0, 10, 0, 2);
        }

        @Test
        @DisplayName("Append an existing style at the end of a style should extend the existing one")
        void appendSameStyle() {
            checkStyle(base.append("text", 2), 12,
                    new String[]{"text"}, 0, 12);
        }
    }
}
