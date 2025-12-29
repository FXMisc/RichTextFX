package org.fxmisc.richtext.model;

import static org.fxmisc.richtext.model.StyleSpansChecker.checkStyle;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import javafx.scene.control.IndexRange;

import org.junit.jupiter.api.Test;

public class ParagraphTest {
    private Paragraph<Void, String, Void> createTextParagraph(TextOps<String, Void> segOps, String text) {
        return new Paragraph<>(null, segOps, segOps.create(text), (Void)null);
    }

    private Paragraph<Void, String, String> createTextParagraph(String text, String style) {
        TextOps<String, String> textOps = SegmentOps.styledTextOps();
        return new Paragraph<>(null, textOps, textOps.create(text), style);
    }

    private Paragraph<Void, String, String> createTextParagraph(TextOps<String, String> segOps, String text, String style) {
        return new Paragraph<>(null, segOps, segOps.create(text), style);
    }

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

    @Test
    public void segmentOperationDoesNotConcat() {
        TextOps<String, Void> noConcatOps = new TextOps<>() {
            @Override public int length(String s) {
                return s.length();
            }
            @Override public char charAt(String s, int index) {return s.charAt(index);}
            @Override public String getText(String d) {return d;}
            @Override public String subSequence(String s, int start, int end) {return s.substring(start, end);}
            @Override public String subSequence(String s, int start) {return s.substring(start);}
            @Override public Optional<String> joinSeg(String currentSeg, String nextSeg) {return Optional.empty();}
            @Override public String createEmptySeg() {return "";}
            @Override public String create(String text) {return text;}
        };
        Paragraph<Void, String, Void> p1 = createTextParagraph(noConcatOps, "A");
        Paragraph<Void, String, Void> p2 = createTextParagraph(noConcatOps, "B");
        assertEquals("AB", p1.concat(p2).getText());
        assertEquals("BA", p2.concat(p1).getText());
    }

    @Test
    public void concatParagraphsWithStyleMerge() {
        // Merge of style is the sum of both styles
        TextOps<String, String> segOps = SegmentOps.styledTextOps((left, right) -> Optional.of(left + "-" + right));
        Paragraph<Void, String, String> p1 = createTextParagraph(segOps, " alpha", "first");
        Paragraph<Void, String, String> p2 = createTextParagraph(segOps, " beta", "second");
        Paragraph<Void, String, String> p3 = p1.concat(p2);
        assertEquals(" alpha beta", p3.getText());
        for (int i = 0; i < p3.getText().length(); i++) {
            assertEquals("first-second", p3.getStyleAtPosition(i));
        }
    }

    @Test
    public void concatTwoParagraphs() {
        TextOps<String, Void> segOps = SegmentOps.styledTextOps();
        Paragraph<Void, String, Void> p1 = createTextParagraph(segOps, " some text");
        Paragraph<Void, String, Void> p2 = createTextParagraph(segOps, " some more text");
        Paragraph<Void, String, Void> empty = createTextParagraph(segOps, "");
        assertEquals(" some text some more text", p1.concat(p2).getText());
        assertEquals(" some more text some text", p2.concat(p1).getText());
        assertEquals(" some text", empty.concat(p1).getText());
        assertEquals(" some more text", empty.concat(p2).getText());
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
			@Override public IndexRange getStyleRange(int position) { return null; }
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

    @Test
    public void trimParagraph() {
        Paragraph<Void, String, String> p1 = createTextParagraph(SegmentOps.styledTextOps(), "Alpha", "MyStyle");
        // Not very consistent that MIN_VALUE is throwing an exception while other negative numbers work
        assertThrows(StringIndexOutOfBoundsException.class, () -> p1.trim(Integer.MIN_VALUE).getText());
        assertEquals("", p1.trim(-10).getText());
        assertEquals("", p1.trim(-1).getText());
        assertEquals("Alpha", p1.trim(Integer.MAX_VALUE).getText());
        assertEquals("", p1.trim(0).getText());
        assertEquals("A", p1.trim(1).getText());
        assertEquals("Alpha", p1.trim(5).getText());
        assertEquals("Alpha", p1.trim(6).getText());

        // Check the style has not changed
        Paragraph<Void, String, String> trimmed = p1.trim(4);
        assertEquals("Alph", trimmed.getText());
        for (int i = 0; i < trimmed.getText().length(); i++) {
            assertEquals("MyStyle", trimmed.getStyleAtPosition(i));
        }
    }

    @Test
    public void deletePartOfParagraph() {
        Paragraph<Void, String, String> paragraph = createTextParagraph(SegmentOps.styledTextOps(), "Elongated", "MyStyle");
        // Start == end
        for (int i = 0; i < paragraph.getText().length(); i++) {
            assertEquals("Elongated", paragraph.delete(i, i).getText());
        }
        assertEquals("Elon", paragraph.delete(4, 9).getText());
        assertThrows(IndexOutOfBoundsException.class, () -> paragraph.delete(4, 10).getText()); // Not consistent with -1
        assertEquals("gated", paragraph.delete(0, 4).getText());
        assertEquals("gated", paragraph.delete(-1, 4).getText());
        assertThrows(StringIndexOutOfBoundsException.class, () -> paragraph.delete(Integer.MIN_VALUE, 4).getText()); // Not very consistent with -1

        // Check style too
        Paragraph<Void, String, String> p2 = paragraph.delete(2, 5);
        assertEquals("Elated", p2.getText());
        for (int i = 0; i < p2.getText().length(); i++) {
            assertEquals("MyStyle", p2.getStyleAtPosition(i));
        }
    }

    @Test
    public void substringCases() {
        // Start only
        Paragraph<Void, String, Void> paragraph = createTextParagraph(SegmentOps.styledTextOps(), "First");
        assertEquals("First", paragraph.substring(0));
        assertEquals("irst", paragraph.substring(1));
        assertEquals("t", paragraph.substring(4));
        assertEquals("", paragraph.substring(5));
        assertThrows(StringIndexOutOfBoundsException.class, () -> paragraph.substring(6));
        assertThrows(StringIndexOutOfBoundsException.class, () -> paragraph.substring(-1));

        // Subsequence is the same but creating a new paragrapha
        assertEquals("First", paragraph.subSequence(0).getText());
        assertEquals("irst", paragraph.subSequence(1).getText());
        assertEquals("t", paragraph.subSequence(4).getText());
        assertEquals("", paragraph.subSequence(5).getText());
        assertThrows(IndexOutOfBoundsException.class, () -> paragraph.subSequence(6));
        assertThrows(IllegalArgumentException.class, () -> paragraph.subSequence(-1)); // Not very consistent with the above 6

        // Start -> end
        assertEquals("First", paragraph.substring(0, 50)); // Consistency issue, outbound index for start throws exception but not the end index
        assertEquals("First", paragraph.substring(0, 5));
        assertEquals("Firs", paragraph.substring(0, 4));
        assertEquals("irs", paragraph.substring(1, 4));
        assertEquals("r", paragraph.substring(2, 3));
        assertEquals("", paragraph.substring(2, 2));
        assertThrows(StringIndexOutOfBoundsException.class, () -> paragraph.substring(3, 2));
        assertEquals("", paragraph.substring(5, 7));
        assertThrows(StringIndexOutOfBoundsException.class, () -> paragraph.substring(6, 7));
    }

    @Test
    public void multipleStyle() {
        Paragraph<Void, String, String> p1 = createTextParagraph("To be or not to be", "text");
        checkStyle(p1, 18, new String[] {"text"}, 0, 18);

        // P1 is immutable, its style hasn't changed, but P2 has now three styles
        Paragraph<Void, String, String> p2 = p1.restyle(9, 12, "keyword");
        checkStyle(p1, 18, new String[] {"text"}, 0, 18);
        checkStyle(p2, 18, new String[] {"text", "keyword", "text"}, 0, 9, 9, 12, 12, 18);

        // Add style over the previous one
        Paragraph<Void, String, String> p3 = p2.restyle(3, 10, "unknown");
        checkStyle(p3, 18, new String[] {"text", "unknown", "keyword", "text"}, 0, 3, 3, 10, 10, 12, 12, 18);

        // Restyle up to the end
        checkStyle(p3.restyle(11, 17, "out"), 18,
                new String[] {"text", "unknown", "keyword", "out", "text"},
                0, 3, 3, 10, 10, 11, 11, 17, 17, 18);

        // Restyle up to the end
        checkStyle(p3.restyle(11, 18, "out"), 18,
                new String[] {"text", "unknown", "keyword", "out"},
                0, 3, 3, 10, 0, 1, 0, 7);

        // Restyle out of bound
        checkStyle(p3.restyle(11, 19, "out"), 19,
                new String[] {"text", "unknown", "keyword", "out"},
                0, 3, 3, 10, 0, 1, 0, 8);

        // From > length, no changes
        checkStyle(p3.restyle(19, 20, "out"), 18,
                new String[] {"text", "unknown", "keyword", "text"},
                0, 3, 3, 10, 10, 12, 12, 18);

        // From == to
        checkStyle(p3.restyle(10, 10, "in"), 18,
                new String[] {"text", "unknown", "keyword", "text"},
                0, 3, 3, 10, 10, 12, 12, 18);

        // To < 0
        // This look very odd
        checkStyle(p3.restyle(-2, -1, "in"), 20,
                new String[] {"in", "text", "unknown", "keyword", "text"},
                0, 1, 1, 4, 4, 11, 11, 13, 13, 20);

        // To < from
        assertThrows(IllegalArgumentException.class, () -> p3.restyle(10, 8, "in"));

        // Style fully the first part starting from negative number
        // This look very odd
        checkStyle(p3.restyle(-1, 3, "replace"), 19,
                new String[] {"replace", "unknown", "keyword", "text"},
                0, 4, 4, 11, 11, 13, 13, 19);

        // Restyle the whole thing
        StyleSpansBuilder<String> builder = new StyleSpansBuilder<>();
        builder.add("first", 9);
        builder.add("second", 4);
        builder.add("last", 5);
        checkStyle(p3.restyle(0, builder.create()), 18,
                new String[] {"first", "second", "last"},
                0, 9, 9, 13, 13, 18);

        // Restyle with one empty
        // Bug
        checkStyle(p3.restyle(0, new StyleSpansBuilder<String>().add("na", 0).create()), 18,
                new String[] {"text", "unknown", "keyword", "text"},
                0, 3, 4, 11, 11, 13, 0, 6);

        // Restyle with empty style span
        // Bug
        StyleSpans<String> emptyStyle = new StyleSpans<>() {
            @Override public Position position(int major, int minor) {return null;}
            @Override public Position offsetToPosition(int offset, Bias bias) {return null;}
            @Override public int length() {return 0;}
            @Override public int getSpanCount() {return 0;}
            @Override public StyleSpan<String> getStyleSpan(int index) {return null;}
            @Override public IndexRange getStyleRange(int position) {return null;}
        };
        checkStyle(p3.restyle(0, emptyStyle), 18,
                new String[] {"text", "unknown", "keyword", "text"},
                0, 3, 4, 11, 11, 13, 12, 18);
    }
}
