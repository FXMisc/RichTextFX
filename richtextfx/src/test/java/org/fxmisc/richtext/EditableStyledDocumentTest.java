package org.fxmisc.richtext;

import static org.junit.Assert.*;

import org.junit.Test;

public class EditableStyledDocumentTest {

    @Test
    public void testConsistencyOfTextWithLength() {
        EditableStyledDocument<String, String> document = new EditableStyledDocument<>("", "");
        document.getText(); // enforce evaluation of text property
        document.getLength(); // enforce evaluation of length property

        document.lengthProperty().addListener(obs -> {
            int length = document.getLength();
            int textLength = document.getText().length();
            assertEquals(length, textLength);
        });

        document.replaceText(0, 0, "A");
    }

    @Test
    public void testConsistencyOfLengthWithText() {
        EditableStyledDocument<String, String> document = new EditableStyledDocument<>("", "");
        document.getText(); // enforce evaluation of text property
        document.getLength(); // enforce evaluation of length property

        document.textProperty().addListener(obs -> {
            int textLength = document.getText().length();
            int length = document.getLength();
            assertEquals(textLength, length);
        });

        document.replaceText(0, 0, "A");
    }

    @Test
    public void testUnixParagraphCount() {
        EditableStyledDocument<String, String> document = new EditableStyledDocument<>("", "");
        String text = "X\nY";
        document.replaceText(0, 0, text);
        assertEquals(2, document.getParagraphs().size());
    }

    @Test
    public void testMacParagraphCount() {
        EditableStyledDocument<String, String> document = new EditableStyledDocument<>("", "");
        String text = "X\rY";
        document.replaceText(0, 0, text);
        assertEquals(2, document.getParagraphs().size());
    }

    @Test
    public void testWinParagraphCount() {
        EditableStyledDocument<String, String> document = new EditableStyledDocument<>("", "");
        String text = "X\r\nY";
        document.replaceText(0, 0, text);
        assertEquals(2, document.getParagraphs().size());
    }

    @Test
    public void testGetTextWithEndAfterNewline() {
        EditableStyledDocument<Boolean, String> doc = new EditableStyledDocument<>(true, "");

        doc.replaceText(0, 0, "123\n");
        String txt1 = doc.getText(0, 4);
        assertEquals(4, txt1.length());

        doc.replaceText(4, 4, "567");
        String txt2 = doc.getText(2, 4);
        assertEquals(2, txt2.length());

        doc.replaceText(4, 4, "\n");
        String txt3 = doc.getText(2, 4);
        assertEquals(2, txt3.length());
    }

    @Test
    public void testWinDocumentLength() {
        EditableStyledDocument<String, String> document = new EditableStyledDocument<>("", "");
        document.replaceText(0, 0, "X\r\nY");
        assertEquals(document.getText().length(), document.getLength());
    }
}
