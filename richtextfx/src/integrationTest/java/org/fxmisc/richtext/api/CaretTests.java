package org.fxmisc.richtext.api;

import javafx.stage.Stage;
import org.fxmisc.richtext.Caret;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CaretTests extends InlineCssTextAreaAppTest {

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        // insure caret is always visible
        area.setShowCaret(Caret.CaretVisibility.ON);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append(i).append("\n");
        }
        area.replaceText(sb.toString());
        area.moveTo(0);
        area.showParagraphAtTop(0);
    }

    @Test
    public void testMoveCaretAndFollowIt() {
        assertTrue(area.getCaretBounds().isPresent());

        // move caret outside of viewport
        area.moveTo(area.getLength());
        area.requestFollowCaret();

        // needed for test to pass
        WaitForAsyncUtils.waitForFxEvents();

        // viewport should update itself so caret is visible again
        assertTrue(area.getCaretBounds().isPresent());
    }

    @Test
    public void testMoveCaretWithoutFollowingIt() {
        assertTrue(area.getCaretBounds().isPresent());

        // move caret outside of viewport
        area.moveTo(area.getLength());

        // caret should not be visible
        assertFalse(area.getCaretBounds().isPresent());
    }
}