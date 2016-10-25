package org.fxmisc.richtext.model;

import static org.fxmisc.richtext.model.ReadOnlyStyledDocument.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class ReadOnlyStyledDocumentTest {

    @Test
    public void testUndo() {
        TextOps<StyledText<String>, String> segOps = StyledText.textOps("");
        ReadOnlyStyledDocument<String, StyledText<String>, String> doc0 = fromString("", "X", "X", segOps);

        doc0.replace(0, 0, fromString("abcd", "Y", "Y", segOps)).exec((doc1, chng1, pchng1) -> {
            // undo chng1
            doc1.replace(chng1.getPosition(), chng1.getInsertionEnd(), from(chng1.getRemoved(), ops)).exec((doc2, chng2, pchng2) -> {
                // we should have arrived at the original document
                assertEquals(doc0, doc2);

                // chng2 should be the inverse of chng1
                assertEquals(chng1.invert(), chng2);
            });
        });
    }

    @Test
    public void deleteNewlineTest() {
        TextOps<StyledText<Void>, Void> segOps = StyledText.textOps(null);
        ReadOnlyStyledDocument<Void, StyledText<Void>, Void> doc0 = fromString("Foo\nBar", null, null, segOps);
        doc0.replace(3, 4, fromString("", null, null, segOps)).exec((doc1, ch, pch) -> {
            List<? extends Paragraph<Void, StyledText<Void>, Void>> removed = pch.getRemoved();
            List<? extends Paragraph<Void, StyledText<Void>, Void>> added = pch.getAdded();
            assertEquals(2, removed.size());
            assertEquals(new NonEmptyParagraph<Void, StyledText<Void>, Void>(null, segOps, segOps.create("Foo", null)), removed.get(0));
            assertEquals(new NonEmptyParagraph<Void, StyledText<Void>, Void>(null, segOps, segOps.create("Bar", null)), removed.get(1));
            assertEquals(1, added.size());
            assertEquals(new NonEmptyParagraph<Void, StyledText<Void>, Void>(null, segOps, segOps.create("FooBar", null)), added.get(0));
        });
    }

    @Test
    public void testRestyle() {
        final String fooBar = "Foo Bar";
        final String and = " and ";
        final String helloWorld = "Hello World";
        TextOps<StyledText<String>, String> segOps = StyledText.textOps("");

        SimpleEditableStyledDocument<String, String> doc0 = new SimpleEditableStyledDocument<>("", "");

        ReadOnlyStyledDocument<String, String> text = fromString(fooBar, "", "bold");
        doc0.replace(doc0.getLength(),  doc0.getLength(), text);

        text = fromString(and, "", "");
        doc0.replace(doc0.getLength(),  doc0.getLength(), text);

        text = fromString(helloWorld, "", "bold");
        doc0.replace(doc0.getLength(),  doc0.getLength(), text);

        StyleSpans<String> styles = doc0.getStyleSpans(4,  17);
        assertThat("Invalid number of Spans", styles.getSpanCount(), equalTo(3));

        StyleSpans<String> newStyles = styles.mapStyles(style -> "italic");
        doc0.setStyleSpans(4, newStyles);

        // assert the new segment structure:
        //  StyledText[text="Foo ", style=bold]
        //  StyledText[text="Bar and Hello", style=italic]
        //  StyledText[text=" World", style=bold]
        List<Segment<String>> result = doc0.getParagraphs().get(0).getSegments();
        assertThat(result.size(), equalTo(3));
        assertThat(result.get(0).getText(), equalTo("Foo "));
        assertThat(result.get(1).getText(), equalTo("Bar and Hello"));
        assertThat(result.get(2).getText(), equalTo(" World"));
    }

}
