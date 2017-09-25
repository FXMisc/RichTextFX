package org.fxmisc.richtext.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class ParagraphTest {

    // Tests that when concatenating two paragraphs,
    // the style of the first one is used for the result.
    // This relates to merging text changes and issue #216.
    @Test
    public void concatEmptyParagraphsTest() {
        TextOps<String, Boolean> segOps = SegmentOps.styledTextOps();
        Paragraph<Void, String, Boolean> p1 = new Paragraph<>(null, segOps, segOps.create(""), true);
        Paragraph<Void, String, Boolean> p2 = new Paragraph<>(null, segOps, segOps.create(""), false);

        Paragraph<Void, String, Boolean> p = p1.concat(p2);

        assertEquals(Boolean.TRUE, p.getStyleAtPosition(0));
    }

    // Relates to #345 and #505: calling `EditableStyledDocument::setStyleSpans` when the style spans
    //  would style an empty paragraph would throw an exception
    @Test
    public void restylingEmptyParagraphViaStyleSpansWorks() {
        TextOps<String, Boolean> segOps = SegmentOps.styledTextOps();
        Paragraph<Void, String, Boolean> p = new Paragraph<>(null, segOps, segOps.createEmptySeg(), false);

        StyleSpansBuilder<Boolean> builder = new StyleSpansBuilder<>();
        builder.add(true, 2);
        StyleSpans<Boolean> spans = builder.create();
        Paragraph<Void, String, Boolean> restyledP = p.restyle(0, spans);

        assertEquals(p, restyledP);
    }

}
