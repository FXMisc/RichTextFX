package org.fxmisc.richtext;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

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

    @Test
    public void testForBug216() {
        // set up area with some styled text content
        boolean initialStyle = false;
        StyledTextAreaModel<String, Boolean> model = new StyledTextAreaModel<>(
                "", initialStyle, new EditableStyledDocument<>("", initialStyle), true);
        model.replaceText("testtest");
        model.setStyle(0, 8, true);

        // add a space styled by initialStyle
        model.setUseInitialStyleForInsertion(true);
        model.insertText(4, " ");

        // add another space
        model.insertText(5, " ");

        // testing that undo/redo don't throw an exception
        model.undo();
        model.redo();
    }

}
