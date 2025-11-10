package org.fxmisc.richtext.model;

import static org.fxmisc.richtext.model.ReadOnlyStyledDocument.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

public class ReadOnlyStyledDocumentTest {
    private static final Void NULL = new Void();

    /** Short for Void:
     * cannot pass in 'null' since compiler will interpret it as a StyleSpans argument to Paragraph's constructor */
    private static class Void { }

    @Test
    public void testUndo() {
        TextOps<String, String> segOps = SegmentOps.styledTextOps();
        ReadOnlyStyledDocument<String, String, String> doc0 = fromString("", "X", "X", segOps);

        doc0.replace(0, 0, fromString("abcd", "Y", "Y", segOps)).exec((doc1, chng1, pchng1) -> {
            // undo chng1
            doc1.replace(chng1.getPosition(), chng1.getInsertionEnd(), from(chng1.getRemoved().data())).exec((doc2, chng2, pchng2) -> {
                // we should have arrived at the original document
                assertEquals(doc0, doc2);

                // chng2 should be the inverse of chng1
                assertEquals(chng1.invert(), chng2);
            });
        });
    }

    @Test
    public void testMultiParagraphFromSegment() {
        TextOps<String, Void> segOps = SegmentOps.styledTextOps();
        ReadOnlyStyledDocument<Void, String, Void> doc0 = fromSegment("Foo\nBar", NULL, NULL, segOps);
        assertEquals( 2, doc0.getParagraphCount() );
    }

    @Test
    public void deleteNewlineTest() {
        TextOps<String, Void> segOps = SegmentOps.styledTextOps();
        ReadOnlyStyledDocument<Void, String, Void> doc0 = fromString("Foo\nBar", NULL, NULL, segOps);
        doc0.replace(3, 4, fromString("", NULL, NULL, segOps)).exec((doc1, ch, pch) -> {
            List<? extends Paragraph<Void, String, Void>> removed = pch.getRemoved();
            List<? extends Paragraph<Void, String, Void>> added = pch.getAdded();
            assertEquals(2, removed.size());
            Paragraph<Void, String, Void> p = new Paragraph<>(NULL, segOps, segOps.create("som"), NULL);
            assertEquals(new Paragraph<>(NULL, segOps, "Foo", NULL), removed.get(0));
            assertEquals(new Paragraph<>(NULL, segOps, "Bar", NULL), removed.get(1));
            assertEquals(1, added.size());
            assertEquals(new Paragraph<>(NULL, segOps, "FooBar", NULL), added.get(0));
        });
    }

    @Test
    public void evaluatePosition() {
        TextOps<String, Void> segOps = SegmentOps.styledTextOps();
        String text = """
                To be, or not to be, that is the question:
                Whether 'tis nobler in the mind to suffer
                The slings and arrows of outrageous fortune,
                Or to take arms against a sea of troubles,""";
        ReadOnlyStyledDocument<Void, String, Void> document = fromString(text, NULL, NULL, segOps);
        assertEquals(0, document.position(0, 0).toOffset());
        assertEquals(2, document.position(0, 2).toOffset());
        assertEquals(42, document.position(0, 42).toOffset());
        assertEquals(43, document.position(1, 0).toOffset());
        assertEquals(45, document.position(1, 2).toOffset());
        assertEquals(87, document.position(2, 2).toOffset());
        assertEquals(132, document.position(3, 2).toOffset());
        assertEquals(132, document.position(3, 2).clamp().toOffset());
        assertEquals(172, document.position(3, 42).toOffset()); // End of the line
        assertThrows(IndexOutOfBoundsException.class, () -> document.position(5, 0).toOffset());
        assertThrows(IndexOutOfBoundsException.class, () -> document.position(-1, 0).toOffset());

        // Following is covering current behaviour to avoid breaking, but these are quite strange
        assertEquals(-1, document.position(0, -1).toOffset());
        assertEquals(-1, document.position(0, -1).clamp().toOffset());
        assertEquals(43, document.position(0, 43).toOffset()); // First paragraph has 42 chars
        assertEquals(43, document.position(0, 43).clamp().toOffset());
        assertEquals(44, document.position(0, 44).toOffset());
        assertEquals(44, document.position(0, 44).clamp().toOffset());
        assertEquals(42, document.position(1, -1).toOffset());
        assertEquals(42, document.position(1, -1).clamp().toOffset());
        assertEquals(173, document.position(4, 0).toOffset()); // There are only 3 paragraphs
        assertEquals(173, document.position(3, 43).toOffset()); // After the end of the line it continues for some reason
        assertEquals(171, document.position(3, 43).clamp().toOffset());
    }

    @Test
    public void testRestyle() {
        // texts
        final String fooBar = "Foo Bar";
        final String and = " and ";
        final String helloWorld = "Hello World";

        // styles
        final String bold = "bold";
        final String empty = "";
        final String italic = "italic";
        TextOps<String, String> segOps = SegmentOps.styledTextOps();

        SimpleEditableStyledDocument<String, String> doc0 = new SimpleEditableStyledDocument<>("", "");

        BiConsumer<String, String> appendStyledText = (text, style) -> {
            ReadOnlyStyledDocument<String, String, String> rosDoc = fromString(text, "", style, segOps);
            doc0.replace(doc0.getLength(), doc0.getLength(), rosDoc);
        };

        appendStyledText.accept(fooBar, bold);
        appendStyledText.accept(and, empty);
        appendStyledText.accept(helloWorld, bold);

        StyleSpans<String> styles = doc0.getStyleSpans(4,  17);
        assertThat("Invalid number of Spans", styles.getSpanCount(), equalTo(3));

        StyleSpans<String> newStyles = styles.mapStyles(style -> italic);
        doc0.setStyleSpans(4, newStyles);

        // assert the new segment structure:
        //  StyledText[text="Foo ", style=bold]
        //  StyledText[text="Bar and Hello", style=italic]
        //  StyledText[text=" World", style=bold]
        StyleSpans<String> spans = doc0.getParagraphs().get(0).getStyleSpans();
        assertThat(spans.getSpanCount(), equalTo(3));
        assertThat(spans.getStyleSpan(0).getStyle(), equalTo(bold));
        assertThat(spans.getStyleSpan(1).getStyle(), equalTo(italic));
        assertThat(spans.getStyleSpan(2).getStyle(), equalTo(bold));
    }

}
