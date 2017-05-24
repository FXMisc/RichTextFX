package org.fxmisc.richtext.keyboard;

import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TypingTests extends InlineCssTextAreaAppTest {

    @Test
    public void typingALetterMovesTheCaretAfterThatInsertedLetter() {
        interact(() -> {
            area.moveTo(0);
            area.clear();
        });

        String userInputtedText = "some text";
        leftClickOnFirstLine().write(userInputtedText);

        assertEquals(userInputtedText, area.getText());
        assertEquals(userInputtedText.length(), area.getCaretPosition());
        assertTrue(area.getSelectedText().isEmpty());
    }

}