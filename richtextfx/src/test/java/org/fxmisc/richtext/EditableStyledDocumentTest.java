package org.fxmisc.richtext;

import static org.junit.Assert.*;

import org.junit.Test;

public class EditableStyledDocumentTest {

    @Test
    public void testConsistencyOfTextWithLength() {
        EditableStyledDocument<String> document = new EditableStyledDocument<>("");
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
        EditableStyledDocument<String> document = new EditableStyledDocument<>("");
        document.getText(); // enforce evaluation of text property
        document.getLength(); // enforce evaluation of length property

        document.textProperty().addListener(obs -> {
            int textLength = document.getText().length();
            int length = document.getLength();
            assertEquals(textLength, length);
        });

        document.replaceText(0, 0, "A");
    }

}
