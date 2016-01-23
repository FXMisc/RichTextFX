package org.fxmisc.richtext;

import static org.junit.Assert.*;
import javafx.embed.swing.JFXPanel;

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

public class StyledTextAreaModelTest {

    @Test
    public void testUndoWithWinNewlines() {
        String text1 = "abc\r\ndef";
        String text2 = "A\r\nB\r\nC";
        StyledTextAreaModel<Collection<String>, Collection<String>> model = new StyledTextAreaModel<>(
                Collections.<String>emptyList(),
                Collections.<String>emptyList()
        );

        model.replaceText(text1);
        model.getUndoManager().forgetHistory();
        model.insertText(0, text2);
        assertEquals("A\nB\nCabc\ndef", model.getText());

        model.undo();
        assertEquals("abc\ndef", model.getText());
    }

}
