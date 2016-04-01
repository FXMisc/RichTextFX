package org.fxmisc.richtext;

import static org.junit.Assert.*;

import org.junit.Test;

public class EditableStyledDocumentImplTest {

    @Test
    public void testConsistencyOfTextWithLength() {
        EditableStyledDocumentImpl<String, String> document = new EditableStyledDocumentImpl<>("", "");
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
        EditableStyledDocumentImpl<String, String> document = new EditableStyledDocumentImpl<>("", "");
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
        EditableStyledDocumentImpl<String, String> document = new EditableStyledDocumentImpl<>("", "");
        String text = "X\nY";
        document.replaceText(0, 0, text);
        assertEquals(2, document.getParagraphs().size());
    }

    @Test
    public void testMacParagraphCount() {
        EditableStyledDocumentImpl<String, String> document = new EditableStyledDocumentImpl<>("", "");
        String text = "X\rY";
        document.replaceText(0, 0, text);
        assertEquals(2, document.getParagraphs().size());
    }

    @Test
    public void testWinParagraphCount() {
        EditableStyledDocumentImpl<String, String> document = new EditableStyledDocumentImpl<>("", "");
        String text = "X\r\nY";
        document.replaceText(0, 0, text);
        assertEquals(2, document.getParagraphs().size());
    }

    @Test
    public void testGetTextWithEndAfterNewline() {
        EditableStyledDocumentImpl<Boolean, String> doc = new EditableStyledDocumentImpl<>(true, "");

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
        EditableStyledDocumentImpl<String, String> document = new EditableStyledDocumentImpl<>("", "");
        document.replaceText(0, 0, "X\r\nY");
        assertEquals(document.getText().length(), document.getLength());
    }
}
