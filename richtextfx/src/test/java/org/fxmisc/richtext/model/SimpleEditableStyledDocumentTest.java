package org.fxmisc.richtext.model;

import static org.junit.Assert.*;

import javafx.scene.control.IndexRange;
import org.junit.Test;

public class SimpleEditableStyledDocumentTest {

    private final TextOps<String, String> segOps = SegmentOps.styledTextOps();

    /**
     * The style of the inserted text will be the style at position
     * {@code start} in the current document.
     */
    private <PS> void replaceText(EditableStyledDocument<PS, String, String> doc, int start, int end, String text) {
        StyledDocument<PS, String, String> styledDoc = ReadOnlyStyledDocument.fromString(
                text, doc.getParagraphStyleAtPosition(start), doc.getStyleAtPosition(start), segOps);
        doc.replace(start, end, styledDoc);
    }

    @Test
    public void testConsistencyOfTextWithLength() {
        SimpleEditableStyledDocument<String, String> document = new SimpleEditableStyledDocument<>("", "");
        document.getText(); // enforce evaluation of text property
        document.getLength(); // enforce evaluation of length property

        document.lengthProperty().addListener(obs -> {
            int length = document.getLength();
            int textLength = document.getText().length();
            assertEquals(length, textLength);
        });

        replaceText(document, 0, 0, "A");
    }

    @Test
    public void testConsistencyOfLengthWithText() {
        SimpleEditableStyledDocument<String, String> document = new SimpleEditableStyledDocument<>("", "");
        document.getText(); // enforce evaluation of text property
        document.getLength(); // enforce evaluation of length property

        document.textProperty().addListener(obs -> {
            int textLength = document.getText().length();
            int length = document.getLength();
            assertEquals(textLength, length);
        });

        replaceText(document, 0, 0, "A");
    }

    @Test
    public void testUnixParagraphCount() {
        SimpleEditableStyledDocument<String, String> document = new SimpleEditableStyledDocument<>("", "");
        String text = "X\nY";
        replaceText(document, 0, 0, text);
        assertEquals(2, document.getParagraphs().size());
    }

    @Test
    public void testMacParagraphCount() {
        SimpleEditableStyledDocument<String, String> document = new SimpleEditableStyledDocument<>("", "");
        String text = "X\rY";
        replaceText(document, 0, 0, text);
        assertEquals(2, document.getParagraphs().size());
    }

    @Test
    public void testWinParagraphCount() {
        SimpleEditableStyledDocument<String, String> document = new SimpleEditableStyledDocument<>("", "");
        String text = "X\r\nY";
        replaceText(document, 0, 0, text);
        assertEquals(2, document.getParagraphs().size());
    }

    @Test
    public void testGetTextWithEndAfterNewline() {
        SimpleEditableStyledDocument<Boolean, String> doc = new SimpleEditableStyledDocument<>(true, "");

        replaceText(doc, 0, 0, "123\n");
        String txt1 = doc.getText(0, 4);
        assertEquals(4, txt1.length());

        replaceText(doc, 4, 4, "567");
        String txt2 = doc.getText(2, 4);
        assertEquals(2, txt2.length());

        replaceText(doc, 4, 4, "\n");
        String txt3 = doc.getText(2, 4);
        assertEquals(2, txt3.length());
    }

    @Test
    public void testWinDocumentLength() {
        SimpleEditableStyledDocument<String, String> document = new SimpleEditableStyledDocument<>("", "");
        replaceText(document, 0, 0, "X\r\nY");
        assertEquals(document.getText().length(), document.getLength());
    }

    @Test
    public void testSetEmptyParagraphStyle() {
        SimpleEditableStyledDocument<String, String> document = new SimpleEditableStyledDocument<>("", "");
        String newParStyle = "new style";
        document.setParagraphStyle(0, newParStyle);
        assertEquals(newParStyle, document.getParagraphStyle(0));
    }

    @Test
    public void testSetNonEmptyParagraphStyle() {
        SimpleEditableStyledDocument<String, String> document = new SimpleEditableStyledDocument<>("", "");
        replaceText(document, 0, 0, "some text");
        String newParStyle = "new style";
        document.setParagraphStyle(0, newParStyle);
        assertEquals(newParStyle, document.getParagraphStyle(0));
    }

    @Test
    public void testGetStyleRangeAtPosition() {
        SimpleEditableStyledDocument<String, String> document = new SimpleEditableStyledDocument<>("", "");
        String first = "some";
        String second = " text";
        replaceText(document, 0, 0, first + second);
        document.setStyle(0, first.length(), "abc");

        IndexRange range = document.getStyleRangeAtPosition(0);
        IndexRange expected = new IndexRange(0, first.length());
        assertEquals(expected, range);

        range = document.getStyleRangeAtPosition(first.length());
        assertEquals(expected, range);

        range = document.getStyleRangeAtPosition(first.length() + 1);
        expected = new IndexRange(first.length(), (first + second).length());
        assertEquals(expected, range);
    }
}
