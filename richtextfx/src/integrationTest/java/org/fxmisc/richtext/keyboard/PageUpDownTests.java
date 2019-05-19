package org.fxmisc.richtext.keyboard;

import javafx.application.Platform;
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

public class PageUpDownTests extends InlineCssTextAreaAppTest {

    private static final String EIGHT_LINES = TextBuildingUtils.buildLines(8);

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        // allow 6 lines to be displayed
        stage.setHeight(90);
        area.replaceText(EIGHT_LINES);
    }

    @Test
    public void page_up_leaves_caret_at_bottom_of_viewport() {
        interact(() -> {
            area.moveTo(5, 0);
            area.requestFollowCaret();
        });
        assertTrue(area.getSelectedText().isEmpty());
        Bounds beforeBounds = area.getCaretBounds().get();

        type(PAGE_UP);

        Platform.runLater(() -> {
            Bounds afterBounds = area.getCaretBounds().get();
            assertEquals(8, area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
            assertTrue(beforeBounds.getMinY() > afterBounds.getMinY());
        });
    }

    @Test
    public void page_down_leaves_caret_at_top_of_viewport() throws Exception {
        interact(() -> {
            area.moveTo(0);
            area.requestFollowCaret();
        });
        assertTrue(area.getSelectedText().isEmpty());
        Bounds beforeBounds = area.getCaretBounds().get();

        type(PAGE_DOWN);

        Platform.runLater(() -> {
            Bounds afterBounds = area.getCaretBounds().get();
            assertEquals(area.getAbsolutePosition(3, 0), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
            assertTrue(beforeBounds.getMinY() < afterBounds.getMinY());
        });
    }

    @Test
    public void shift_page_up_leaves_caret_at_bottom_of_viewport_and_makes_selection() {
        interact(() -> {
            area.moveTo(5, 0);
            area.requestFollowCaret();
        });
        assertTrue(area.getSelectedText().isEmpty());
        Bounds beforeBounds = area.getCaretBounds().get();

        press(SHIFT).type(PAGE_UP).release(SHIFT);

        Platform.runLater(() -> {
            Bounds afterBounds = area.getCaretBounds().get();
            assertEquals(8, area.getCaretPosition());
            assertEquals(area.getText(0, 0, 5, 0), area.getSelectedText());
            assertTrue(beforeBounds.getMinY() > afterBounds.getMinY());
        });
    }

    @Test
    public void shift_page_down_leaves_caret_at_top_of_viewport_and_makes_selection() {
        interact(() -> {
            area.moveTo(0);
            area.requestFollowCaret();
        });
        assertTrue(area.getSelectedText().isEmpty());
        Bounds beforeBounds = area.getCaretBounds().get();

        press(SHIFT).type(PAGE_DOWN).release(SHIFT);

        Platform.runLater(() -> {
            Bounds afterBounds = area.getCaretBounds().get();
            assertEquals(area.getAbsolutePosition(3, 0), area.getCaretPosition());
            assertEquals(area.getText(0, 0, 3, 0), area.getSelectedText());
            assertTrue(beforeBounds.getMinY() < afterBounds.getMinY());
        });
    }

}

