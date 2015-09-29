package org.fxmisc.richtext;

import static org.junit.Assert.*;
import javafx.embed.swing.JFXPanel;

import org.junit.Test;

public class StyledTextAreaTest {

    @Test
    public void testUndoWithWinNewlines() {
        new JFXPanel(); // initialize JavaFX

        String text1 = "abc\r\ndef";
        String text2 = "A\r\nB\r\nC";
        StyleClassedTextArea area = new StyleClassedTextArea();

        area.replaceText(text1);
        area.getUndoManager().forgetHistory();
        area.insertText(0, text2);
        assertEquals("A\nB\nCabc\ndef", area.getText());

        area.undo();
        assertEquals("abc\ndef", area.getText());
    }

}
