package org.fxmisc.richtext.model;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CustomObjectTest {

    @Test
    public void testLinkedImageCreation() {
        SimpleEditableStyledDocument<Boolean, String> doc = 
                new SimpleEditableStyledDocument<>(true, "");
        StyledDocument<Boolean, String> customObj = 
                ReadOnlyStyledDocument.from(new LinkedImage<String>("sample.png", ""), true);
        doc.replace(0, 0, customObj);
        assertEquals(1, doc.getLength());

        Paragraph<Boolean, String> para = doc.getParagraphs().get(0);
        Object x = para.getSegments().get(0);
        assertThat(x, instanceOf(LinkedImage.class));
    }

    @Test
    public void testMultipleSegments() {
        SimpleEditableStyledDocument<Boolean, String> doc = new SimpleEditableStyledDocument<>(true, "");

        final String helloWorld = "Hello World";
        final String helloMoon = "Hello Moon";

        StyledDocument<Boolean, String> text = ReadOnlyStyledDocument.fromString(helloWorld, true, "");
        doc.replace(0, 0, text);

        StyledDocument<Boolean, String> customObj = ReadOnlyStyledDocument.from(new LinkedImage<String>("sample.png", ""), true);
        doc.replace(doc.getLength(), doc.getLength(), customObj);

        StyledDocument<Boolean, String> text2 = ReadOnlyStyledDocument.fromString(helloMoon, true, "");
        doc.replace(doc.getLength(), doc.getLength(), text2);

        // Assert that the document now contains one paragraph with three segments

        assertThat(doc.getParagraphs().size(), equalTo(1));

        Paragraph<Boolean, String> p = doc.getParagraphs().get(0);
        List<Segment<String>> segs = p.getSegments();

        assertThat(segs.size(), equalTo(3));
        assertThat(segs.get(0).getText(), equalTo(helloWorld));
        assertThat(segs.get(1).getText(), equalTo("\ufffc"));
        assertThat(segs.get(2).getText(), equalTo(helloMoon));
    }
    
    
    @Test
    public void testRestyle() {
        SimpleEditableStyledDocument<Boolean, String> doc = new SimpleEditableStyledDocument<>(true, "");

        final String helloWorld = "Hello World";
        final String helloMoon = "Hello Moon";

        StyledDocument<Boolean, String> text = ReadOnlyStyledDocument.fromString(helloWorld, true, "bold");
        doc.replace(0, 0, text);

        StyledDocument<Boolean, String> customObj = ReadOnlyStyledDocument.from(new LinkedImage<String>("sample.png", ""), true);
        doc.replace(doc.getLength(), doc.getLength(), customObj);

        StyledDocument<Boolean, String> text2 = ReadOnlyStyledDocument.fromString(helloMoon, true, "bold");
        doc.replace(doc.getLength(), doc.getLength(), text2);

        // The document now contains one paragraph with three segments
        // Restyle part of the document:

        StyleSpans<String> styles = doc.getStyleSpans(6,  17);
        assertThat("Invalid number of Spans", styles.getSpanCount(), equalTo(3));

        StyleSpans<String> newStyles = styles.mapStyles(style -> "italic");
        doc.setStyleSpans(6, newStyles);

        // Assert that the document now contains one paragraph with five segments
        //  StyledText[text="Hello ", style=bold]
        //  StyledText[text="World", style=italic]
        //  LinkedImage[path=sample.png]
        //  StyledText[text="Hello", style=italic]
        //  StyledText[text=" Moon", style=bold]

        assertThat(doc.getParagraphs().size(), equalTo(1));
        List<Segment<String>> segs = doc.getParagraphs().get(0).getSegments();

        assertThat(segs.size(), equalTo(5));
        assertThat(segs.get(0).getText(), equalTo("Hello "));
        assertThat(segs.get(1).getText(), equalTo("World"));
        assertThat(segs.get(2).getText(), equalTo("\ufffc"));
        assertThat(segs.get(2),           instanceOf(LinkedImage.class));
        assertThat(segs.get(3).getText(), equalTo("Hello"));
        assertThat(segs.get(4).getText(), equalTo(" Moon"));
    }

}
