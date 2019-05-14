package org.fxmisc.richtext.model;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;

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
    // StyleSpans being applied to an empty paragraph results in an Exception
    @Test
    public void restylingEmptyParagraphViaEmptyStyleSpansWorks() {

    	Collection<String> test = Collections.singleton("test");
        TextOps<String, Collection<String>> segOps = SegmentOps.styledTextOps();
        Paragraph<Void, String, Collection<String>> p = new Paragraph<>(null, segOps, "", test);
        assertEquals( 0, p.length() );

        StyleSpans<Collection<String>> spans = new StyleSpans<Collection<String>>()
		{
			@Override public Position position( int major, int minor ) { return null; }
			@Override public Position offsetToPosition( int offset, Bias bias ) { return null; }
			@Override public StyleSpan<Collection<String>> getStyleSpan( int index ) { return null; }
			@Override public int getSpanCount() { return 0; }
			@Override public int length() { return 0; }
		};

        Paragraph<Void, String, Collection<String>> restyledP = p.restyle(0, spans);
        assertEquals( test, restyledP.getStyleSpans().getStyleSpan( 0 ).getStyle() );
    }
	
    // Relates to #815 where an undo after deleting a portion of styled text in a multi-
    // styled paragraph causes an exception in UndoManager receiving an unexpected change.
    @Test
    public void multiStyleParagraphReturnsCorrect_subSequenceOfLength() {

    	Collection<String> test = Collections.singleton("test");
        TextOps<String, Collection<String>> segOps = SegmentOps.styledTextOps();
        StyleSpansBuilder ssb = new StyleSpansBuilder<>(2);
        ssb.add( Collections.EMPTY_LIST, 8 );
        ssb.add( test, 8 );
        
        Paragraph<Void, String, Collection<String>> p = new Paragraph<>(null, segOps, "noStyle hasStyle", ssb.create());
        assertEquals( test, p.subSequence( p.length() ).getStyleOfChar(0) );
    }
}
