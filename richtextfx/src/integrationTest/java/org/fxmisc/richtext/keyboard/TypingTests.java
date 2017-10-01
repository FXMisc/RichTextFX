package org.fxmisc.richtext.keyboard;

import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TypingTests extends InlineCssTextAreaAppTest {

    @Test
    public void typing_a_letter_moves_caret_after_the_inserted_letter() {
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