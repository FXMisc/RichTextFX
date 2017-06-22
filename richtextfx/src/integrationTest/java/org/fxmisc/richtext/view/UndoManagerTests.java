package org.fxmisc.richtext.view;

import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.util.UndoUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UndoManagerTests extends InlineCssTextAreaAppTest {

    @Test
    public void preventMergeOfIncomingChangeAfterPeriodOfUserInactivity() {
        String text1 = "text1";
        String text2 = "text2";

        long periodOfUserInactivity = UndoUtils.DEFAULT_PREVENT_MERGE_DELAY.toMillis() + 300L;

        write(text1);
        sleep(periodOfUserInactivity);
        write(text2);

        interact(area::undo);
        assertEquals(text1, area.getText());

        interact(area::undo);
        assertEquals("", area.getText());
    }
}
