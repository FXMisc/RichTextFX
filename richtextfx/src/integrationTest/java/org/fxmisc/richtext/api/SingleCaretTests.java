package org.fxmisc.richtext.api;

import javafx.stage.Stage;
import org.fxmisc.richtext.Caret;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SingleCaretTests extends InlineCssTextAreaAppTest {

    private static final String MANY_PARS_OF_TEXT;

    static {
        int totalLines = 20;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalLines; i++) {
            sb.append(i).append("\n");
        }
        sb.append(totalLines);
        MANY_PARS_OF_TEXT = sb.toString();
    }

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        stage.setHeight(50);

        // insure caret is always visible
        area.setShowCaret(Caret.CaretVisibility.ON);
        area.replaceText(MANY_PARS_OF_TEXT);
        area.moveTo(0);
        area.showParagraphAtTop(0);
    }

    @Test
    public void caret_bounds_are_present_after_moving_caret_and_following_it() {
        assertTrue(area.getCaretBounds().isPresent());

        // move caret outside of viewport
        interact(() -> {
            area.moveTo(area.getLength());
            area.requestFollowCaret();
        });

        // needed for test to pass
        WaitForAsyncUtils.waitForFxEvents();

        // viewport should update itself so caret is visible again
        assertTrue(area.getCaretBounds().isPresent());
    }

    @Test
    public void caret_bounds_are_absent_after_moving_caret_without_following_it() {
        assertTrue(area.getCaretBounds().isPresent());

        // move caret outside of viewport
        interact(() -> area.moveTo(area.getLength()));

        // caret should not be visible
        assertFalse(area.getCaretBounds().isPresent());
    }
}