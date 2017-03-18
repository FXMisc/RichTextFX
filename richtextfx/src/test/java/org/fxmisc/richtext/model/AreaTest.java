package org.fxmisc.richtext.model;

import org.fxmisc.richtext.InlineCssTextArea;
import org.junit.Test;

public class AreaTest {

    private InlineCssTextArea area = new InlineCssTextArea();

    @Test
    public void deletingTextThatWasJustInsertedShouldNotMergeTheTwoChanges() {
        area.replaceText(0, 0, "text");
        area.replaceText(0, area.getLength(), "");
        area.undo();
    }
}
