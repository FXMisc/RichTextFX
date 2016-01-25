package org.fxmisc.richtext;

import static org.junit.Assert.*;

import org.junit.Test;

public class ParagraphTest {

    // Tests that when concatenating two paragraphs,
    // the style of the first one is used for the result.
    // This relates to merging text changes and issue #216.
    @Test
    public void concatEmptyParagraphsTest() {
        Paragraph<Void, Boolean> p1 = new Paragraph<>(null, "", true);
        Paragraph<Void, Boolean> p2 = new Paragraph<>(null, "", false);

        Paragraph<Void, Boolean> p = p1.concat(p2);

        assertEquals(Boolean.TRUE, p.getStyleAtPosition(0));
    }

}
