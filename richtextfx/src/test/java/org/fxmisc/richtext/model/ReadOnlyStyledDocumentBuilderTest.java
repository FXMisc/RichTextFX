package org.fxmisc.richtext.model;

import org.junit.Test;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ReadOnlyStyledDocumentBuilderTest {

    private static final TextOps<String, String> SEGMENT_OPS = SegmentOps.styledTextOps();

    @Test
    public void adding_single_segment_single_style_single_paragraph_works() {
        String text = "a";
        String paragraphStyle = "ps style";
        String textStyle = "seg style";

        ReadOnlyStyledDocument<String, String, String> rosd = ReadOnlyStyledDocumentBuilder.constructDocument(
                SEGMENT_OPS, paragraphStyle, builder -> builder.addParagraph(text, textStyle)
        );

        assertEquals(1, rosd.getParagraphCount());

        Paragraph<String, String, String> p = rosd.getParagraph(0);
        assertEquals(text, p.getText());
        assertEquals(textStyle, p.getStyleSpans().getStyleSpan(0).getStyle());
        assertEquals(paragraphStyle, p.getParagraphStyle());
    }

    @Test
    public void adding_multiple_styled_segment_single_paragraph_works() {
        String text = "a";
        String textStyle = "a style";
        String word = "word";
        String wordStyle = "word style";
        String paragraphStyle = "ps style";

        List<StyledSegment<String, String>> styledSegList = new ArrayList<>(2);
        styledSegList.addAll(Arrays.asList(
                new StyledSegment<>(text, textStyle),
                new StyledSegment<>(word, wordStyle)
        ));

        ReadOnlyStyledDocument<String, String, String> rosd = ReadOnlyStyledDocumentBuilder.constructDocument(
                SEGMENT_OPS, paragraphStyle, builder -> builder.addParagraph(styledSegList)
        );

        assertEquals(1, rosd.getParagraphCount());

        Paragraph<String, String, String> p = rosd.getParagraph(0);
        assertEquals(1, p.getSegments().size());
        assertEquals(text + word, p.getText());
        assertEquals(textStyle, p.getStyleSpans().getStyleSpan(0).getStyle());
        assertEquals(paragraphStyle, p.getParagraphStyle());
    }

    @Test
    public void adding_single_segment_single_styleSpans_single_paragraph_works() {
        String text = "a";
        String paragraphStyle = "ps style";
        String textStyle = "seg style";

        StyleSpans<String> spans = StyleSpans.singleton(textStyle, text.length());

        ReadOnlyStyledDocument<String, String, String> rosd = ReadOnlyStyledDocumentBuilder.constructDocument(
                SEGMENT_OPS, paragraphStyle, builder -> builder.addParagraph(text, spans)
        );

        assertEquals(1, rosd.getParagraphCount());

        Paragraph<String, String, String> p = rosd.getParagraph(0);
        assertEquals(text, p.getText());
        assertEquals(textStyle, p.getStyleSpans().getStyleSpan(0).getStyle());
        assertEquals(paragraphStyle, p.getParagraphStyle());
    }

    @Test
    public void adding_single_segment_list_single_styleSpans_single_paragraph_works() {
        String text = "a";
        String paragraphStyle = "ps style";
        String textStyle = "seg style";

        List<String> segmentList = Collections.singletonList(text);
        StyleSpans<String> spans = StyleSpans.singleton(textStyle, text.length());

        ReadOnlyStyledDocument<String, String, String> rosd = ReadOnlyStyledDocumentBuilder.constructDocument(
                SEGMENT_OPS, paragraphStyle, builder -> builder.addParagraph(segmentList, spans)
        );

        assertEquals(1, rosd.getParagraphCount());

        Paragraph<String, String, String> p = rosd.getParagraph(0);
        assertEquals(text, p.getText());
        assertEquals(textStyle, p.getStyleSpans().getStyleSpan(0).getStyle());
        assertEquals(paragraphStyle, p.getParagraphStyle());
    }

    @Test
    public void adding_single_segment_single_style_without_par_style_paragraph_list_works() {
        String text = "a";
        String paragraphStyle = "ps style";
        String textStyle = "seg style";

        List<List<String>> singletonList = Collections.singletonList(
                Collections.singletonList(text)
        );
        StyleSpans<String> spans = StyleSpans.singleton(textStyle, text.length());

        ReadOnlyStyledDocument<String, String, String> rosd = ReadOnlyStyledDocumentBuilder.constructDocument(
                SEGMENT_OPS, paragraphStyle, builder -> builder.addParagraphs(singletonList, spans)
        );

        assertEquals(1, rosd.getParagraphCount());

        Paragraph<String, String, String> p = rosd.getParagraph(0);
        assertEquals(text, p.getText());
        assertEquals(textStyle, p.getStyleSpans().getStyleSpan(0).getStyle());
        assertEquals(paragraphStyle, p.getParagraphStyle());
    }

    @Test
    public void adding_single_segment_single_style_with_par_style_paragraph_list_works() {
        String text = "a";
        String paragraphStyle = "ps style";
        String textStyle = "seg style";

        List<Tuple2<String, List<String>>> singletonList = Collections.singletonList(
                Tuples.t(paragraphStyle, Collections.singletonList(text))
        );
        StyleSpans<String> spans = StyleSpans.singleton(textStyle, text.length());

        ReadOnlyStyledDocument<String, String, String> rosd = ReadOnlyStyledDocumentBuilder.constructDocument(
                SEGMENT_OPS, paragraphStyle, builder -> builder.addParagraphs0(singletonList, spans)
        );

        assertEquals(1, rosd.getParagraphCount());

        Paragraph<String, String, String> p = rosd.getParagraph(0);
        assertEquals(text, p.getText());
        assertEquals(textStyle, p.getStyleSpans().getStyleSpan(0).getStyle());
        assertEquals(paragraphStyle, p.getParagraphStyle());
    }

    @Test
    public void attempting_to_build_ReadOnlyStyledDocument_using_empty_paragraph_list_throws_exception() {
        ReadOnlyStyledDocumentBuilder<String, String, String> builder = new ReadOnlyStyledDocumentBuilder<>(
                SEGMENT_OPS, "ps style"
        );
        try {
            builder.build();
            fail("Cannot build a ReadOnlyStyledDocument with an empty list of paragraphs");
        } catch (IllegalStateException e) {
            // cannot build a ROSD with an empty list of paragraphs
        }
    }

    @Test
    public void using_a_builder_more_than_once_throws_exception() {
        ReadOnlyStyledDocumentBuilder<String, String, String> builder = new ReadOnlyStyledDocumentBuilder<>(
                SEGMENT_OPS, "ps style"
        );
        builder.addParagraph("text", "text style");
        builder.build();
        try {
            builder.build();
            fail("Cannot use a single ReadOnlyStyledDocumentBuilder to build multiple ROSDs.");
        } catch (IllegalStateException e) {
            // each builder has a one-time usage
        }
    }

    @Test
    public void creating_paragraph_with_different_segment_and_style_length_throws_exception() {
        String text = "a";
        int textStyle = 1;

        List<List<String>> singletonList = Collections.singletonList(
                Collections.singletonList(text)
        );
        int segmentLength = text.length();
        int spanLength = segmentLength + 10;
        StyleSpans<Integer> spans = StyleSpans.singleton(textStyle, spanLength);

        ReadOnlyStyledDocumentBuilder<Integer, String, Integer> builder = new ReadOnlyStyledDocumentBuilder<>(
                SegmentOps.styledTextOps(), 0
        );

        assertTrue(segmentLength != spanLength);

        try {
            builder.addParagraphs(singletonList, spans);
            fail("Style spans length must equal the length of all segments");
        } catch (IllegalArgumentException e) {
            // style spans' length is more than the length of all segments
        }
    }
}
