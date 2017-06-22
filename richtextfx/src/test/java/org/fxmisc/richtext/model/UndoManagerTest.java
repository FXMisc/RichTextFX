package org.fxmisc.richtext.model;

import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.StyledTextArea;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class UndoManagerTest {

    private InlineCssTextArea area = new InlineCssTextArea();

    @Test
    public void testUndoWithWinNewlines() {
        final TextOps<StyledText<Collection<String>>, Collection<String>> segOps = StyledText.textOps();

        String text1 = "abc\r\ndef";
        String text2 = "A\r\nB\r\nC";

        area.replaceText(text1);
        area.getUndoManager().forgetHistory();
        area.insertText(0, text2);
        assertEquals("A\nB\nCabc\ndef", area.getText());

        area.undo();
        assertEquals("abc\ndef", area.getText());
    }

    @Test
    public void testForBug216() {
        final TextOps<StyledText<Boolean>, Boolean> segOps = StyledText.textOps();

        // set up area with some styled text content
        boolean initialStyle = false;
        StyledTextArea<String, Boolean> area = new StyledTextArea<>(
                "", (t, s) -> {}, initialStyle, (t, s) -> {},
                new SimpleEditableStyledDocument<>("", initialStyle), true);
        area.replaceText("testtest");
        area.setStyle(0, 8, true);

        // add a space styled by initialStyle
        area.setUseInitialStyleForInsertion(true);
        area.insertText(4, " ");

        // add another space
        area.insertText(5, " ");

        // testing that undo/redo don't throw an exception
        area.undo();
        area.redo();
    }
}
