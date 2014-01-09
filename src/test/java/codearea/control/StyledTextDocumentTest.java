package codearea.control;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StyledTextDocumentTest {

    @Test
    public void testConsistencyOfTextWithLength() {
        StyledTextDocument<String> document = new StyledTextDocument<>("");
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
        StyledTextDocument<String> document = new StyledTextDocument<>("");
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
