package org.fxmisc.richtext.keyboard;

import javafx.geometry.Bounds;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.TextBuildingUtils;
import org.junit.Test;

import static javafx.scene.input.KeyCode.PAGE_DOWN;
import static javafx.scene.input.KeyCode.PAGE_UP;
import static javafx.scene.input.KeyCode.SHIFT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.testfx.util.WaitForAsyncUtils;

public class PageUpDownTests extends InlineCssTextAreaAppTest {

    private static final String EIGHT_LINES = TextBuildingUtils.buildLines(8);

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        clickOn( area );

        // allow 6 lines to be displayed
        stage.setHeight(90);
        area.replaceText(EIGHT_LINES);
    }

    @Test
    public void page_up_leaves_caret_at_BOTTOM_of_viewport_when_FIRST_line_NOT_visible() {
        interact(() -> {
            area.moveTo(7, 0);
            area.requestFollowCaret();
        });
        Bounds beforeBounds = area.getCaretBounds().get();

        type(PAGE_UP);

        Bounds afterBounds = area.getCaretBounds().get();
        assertEquals(beforeBounds.getMinY(), afterBounds.getMinY(), 0);
        assertEquals(5, area.getCurrentParagraph());
        WaitForAsyncUtils.waitForFxEvents(10);
    }

    @Test
    public void page_up_leaves_caret_at_TOP_of_viewport_when_FIRST_line_IS_visible() {
        interact(() -> {
            area.moveTo(4, 0);
            area.requestFollowCaret();
        });

        type(PAGE_UP);

        assertEquals(0, area.getCurrentParagraph());
    }

    @Test
    public void page_down_leaves_caret_at_TOP_of_viewport_when_LAST_line_NOT_visible() throws Exception {
        interact(() -> {
            area.moveTo(0);
            area.requestFollowCaret();
        });
        Bounds beforeBounds = area.getCaretBounds().get();

        type(PAGE_DOWN);

        Bounds afterBounds = area.getCaretBounds().get();
        assertEquals(beforeBounds.getMinY(), afterBounds.getMinY(), 0);
        assertEquals(2, area.getCurrentParagraph());
        WaitForAsyncUtils.waitForFxEvents(10);
    }

    @Test
    public void page_down_leaves_caret_at_BOTTOM_of_viewport_when_LAST_line_IS_visible() throws Exception {
        interact(() -> {
            area.showParagraphAtTop(3);
            area.moveTo(3, 0);
        });

        type(PAGE_DOWN);

        assertEquals(7, area.getCurrentParagraph());
        assertEquals(15, area.getCaretPosition());
    }

    @Test
    public void shift_page_up_leaves_caret_at_bottom_of_viewport_and_makes_selection() {
        interact(() -> {
            area.moveTo(7, 0);
            area.requestFollowCaret();
        });
        Bounds beforeBounds = area.getCaretBounds().get();

        press(SHIFT).type(PAGE_UP).release(SHIFT);

        Bounds afterBounds = area.getCaretBounds().get();
        assertEquals(beforeBounds.getMinY(), afterBounds.getMinY(), 0);
        assertEquals(area.getText(5, 0, 7, 0), area.getSelectedText());
        assertEquals(10, area.getCaretPosition());
        WaitForAsyncUtils.waitForFxEvents(10);
    }

    @Test
    public void shift_page_down_leaves_caret_at_top_of_viewport_and_makes_selection() {
        interact(() -> {
            area.moveTo(1);
            area.requestFollowCaret();
        });
        assertTrue(area.getSelectedText().isEmpty());
        Bounds beforeBounds = area.getCaretBounds().get();

        press(SHIFT).type(PAGE_DOWN).release(SHIFT);

        Bounds afterBounds = area.getCaretBounds().get();
        assertEquals(beforeBounds.getMinY(), afterBounds.getMinY(), 0);
        assertEquals(area.getText(0, 1, 2, 1), area.getSelectedText());
        assertEquals(5, area.getCaretPosition());
        WaitForAsyncUtils.waitForFxEvents(10);
    }

}

