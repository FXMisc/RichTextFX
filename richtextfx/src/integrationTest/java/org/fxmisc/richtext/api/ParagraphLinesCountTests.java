package org.fxmisc.richtext.api;

import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ParagraphLinesCountTests extends InlineCssTextAreaAppTest {

    @Test
    public void multiLineReturnsCorrectCount() {
        String[] lines = {
                "01 02 03 04 05",
                "11 12 13 14 15",
                "21 22 23 24 25"
        };
        interact(() -> {
            area.setWrapText(true);
            stage.setWidth(120);
            area.replaceText(String.join(" ", lines));
        });

        assertEquals(3, area.getParagraphLinesCount(0));
    }

    @Test
    public void singleLineReturnsOne() {
        interact(() -> area.replaceText("some text"));
        assertFalse(area.isWrapText());

        assertEquals(1, area.getParagraphLinesCount(0));
    }

}