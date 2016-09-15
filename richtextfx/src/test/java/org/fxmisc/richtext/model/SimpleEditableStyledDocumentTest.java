package org.fxmisc.richtext.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class SimpleEditableStyledDocumentTest {


    /**
     * The style of the inserted text will be the style at position
     * {@code start} in the current document.
     * @param ops 
     */
    private <PS, SEG, S> void replaceText(EditableStyledDocument<PS, SEG, S> doc, int start, int end, String text, TextOps<SEG, S> segOps) {
        StyledDocument<PS, SEG, S> styledDoc = ReadOnlyStyledDocument.fromString(
                text, doc.getParagraphStyleAtPosition(start), doc.getStyleAtPosition(start), segOps);
        doc.replace(start, end, styledDoc);
    }


    private final TextOps<StyledText<String>, String> segOps = StyledText.textOps();

    @Test
    public void testConsistencyOfTextWithLength() {
        SimpleEditableStyledDocument<String, StyledText<String>, String> document = new SimpleEditableStyledDocument<>("", "", segOps);
        document.getText(); // enforce evaluation of text property
        document.getLength(); // enforce evaluation of length property

        document.lengthProperty().addListener(obs -> {
            int length = document.getLength();
            int textLength = document.getText().length();
            assertEquals(length, textLength);
        });

        replaceText(document, 0, 0, "A", segOps);
    }

    @Test
    public void testConsistencyOfLengthWithText() {
        SimpleEditableStyledDocument<String, StyledText<String>, String> document = new SimpleEditableStyledDocument<>("", "", segOps);
        document.getText(); // enforce evaluation of text property
        document.getLength(); // enforce evaluation of length property

        document.textProperty().addListener(obs -> {
            int textLength = document.getText().length();
            int length = document.getLength();
            assertEquals(textLength, length);
        });

        replaceText(document, 0, 0, "A", segOps);
    }

    @Test
    public void testUnixParagraphCount() {
        SimpleEditableStyledDocument<String, StyledText<String>, String> document = new SimpleEditableStyledDocument<>("", "", segOps);
        String text = "X\nY";
        replaceText(document, 0, 0, text, segOps);
        assertEquals(2, document.getParagraphs().size());
    }

    @Test
    public void testMacParagraphCount() {
        SimpleEditableStyledDocument<String, StyledText<String>, String> document = new SimpleEditableStyledDocument<>("", "", segOps);
        String text = "X\rY";
        replaceText(document, 0, 0, text, segOps);
        assertEquals(2, document.getParagraphs().size());
    }

    @Test
    public void testWinParagraphCount() {
        SimpleEditableStyledDocument<String, StyledText<String>, String> document = new SimpleEditableStyledDocument<>("", "", segOps);
        String text = "X\r\nY";
        replaceText(document, 0, 0, text, segOps);
        assertEquals(2, document.getParagraphs().size());
    }

    @Test
    public void testGetTextWithEndAfterNewline() {
        SimpleEditableStyledDocument<Boolean, StyledText<String>, String> doc = new SimpleEditableStyledDocument<>(true, "", segOps);

        replaceText(doc, 0, 0, "123\n", segOps);
        String txt1 = doc.getText(0, 4);
        assertEquals(4, txt1.length());

        replaceText(doc, 4, 4, "567", segOps);
        String txt2 = doc.getText(2, 4);
        assertEquals(2, txt2.length());

        replaceText(doc, 4, 4, "\n", segOps);
        String txt3 = doc.getText(2, 4);
        assertEquals(2, txt3.length());
    }

    @Test
    public void testWinDocumentLength() {
        SimpleEditableStyledDocument<String, StyledText<String>, String> document = new SimpleEditableStyledDocument<>("", "", segOps);
        replaceText(document, 0, 0, "X\r\nY", segOps);
        assertEquals(document.getText().length(), document.getLength());
    }
}
