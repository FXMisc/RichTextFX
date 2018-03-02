package org.fxmisc.richtext.model;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;

import org.fxmisc.richtext.StyleClassedTextArea;
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

    // Relates to #345 and #505: calling `EditableStyledDocument::setStyleSpans`
    // when styling an empty paragraph would throw an exception.
    // Also relates to #449: where empty paragraphs were being skipped.
    @Test
    public void restylingEmptyParagraphViaStyleSpansWorks() {
        TextOps<String, Boolean> segOps = SegmentOps.styledTextOps();
        Paragraph<Void, String, Boolean> p = new Paragraph<>(null, segOps, segOps.createEmptySeg(), false);
        assertEquals( 0, p.length() );

        StyleSpansBuilder<Boolean> builder = new StyleSpansBuilder<>();
        builder.add(true, 2);
        StyleSpans<Boolean> spans = builder.create();
        Paragraph<Void, String, Boolean> restyledP = p.restyle(0, spans);

        assertTrue(restyledP.getStyleSpans().styleStream().allMatch( b -> b ));
    }

    // Relates to #696 (caused by #685, coming from #449) where an empty
    // style being applied to an empty paragraph results in an Exception
    @Test
    public void restylingEmptyParagraphViaEmptyStyleSpansWorks() {

        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        spansBuilder.add(Collections.singleton("cls1"), 1);
        spansBuilder.add(Collections.singleton("cls2"), 3);
        spansBuilder.add(Collections.emptyList(), 1);		// critical trigger
        StyleSpans ss = spansBuilder.create();

        StyleClassedTextArea area = new StyleClassedTextArea();
        area.replaceText("* a\n\n");
        area.setStyleSpans(0, ss);							// would fail here
    }
}
