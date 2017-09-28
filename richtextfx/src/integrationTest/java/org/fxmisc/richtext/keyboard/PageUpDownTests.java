package org.fxmisc.richtext.keyboard;

import javafx.geometry.Bounds;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;

import static javafx.scene.input.KeyCode.PAGE_DOWN;
import static javafx.scene.input.KeyCode.PAGE_UP;
import static javafx.scene.input.KeyCode.SHIFT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PageUpDownTests extends InlineCssTextAreaAppTest {

    private static final String EIGHT_LINES;

    static {
        StringBuilder sb = new StringBuilder();
        int totalLines = 8;
        for (int i = 0; i < totalLines - 1; i++) {
            sb.append(i).append("\n");
        }
        sb.append(totalLines);
        EIGHT_LINES = sb.toString();
    }

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        // allow 6 lines to be displayed
        stage.setHeight(90);
        area.replaceText(EIGHT_LINES);
    }

    @Test
    public void page_up_moves_caret_to_top_of_viewport() {
        interact(() -> {
            area.moveTo(5, 0);
            area.requestFollowCaret();
        });
        assertTrue(area.getSelectedText().isEmpty());
        Bounds beforeBounds = area.getCaretBounds().get();

        type(PAGE_UP);

        Bounds afterBounds = area.getCaretBounds().get();
        assertEquals(0, area.getCaretPosition());
        assertTrue(area.getSelectedText().isEmpty());
        assertTrue(beforeBounds.getMinY() > afterBounds.getMinY());
    }

    @Test
    public void page_down_moves_caret_to_bottom_of_viewport() throws Exception {
        interact(() -> {
            area.moveTo(0);
            area.requestFollowCaret();
        });
        assertTrue(area.getSelectedText().isEmpty());
        Bounds beforeBounds = area.getCaretBounds().get();

        type(PAGE_DOWN);

        Bounds afterBounds = area.getCaretBounds().get();
        assertEquals(area.getAbsolutePosition(5, 0), area.getCaretPosition());
        assertTrue(area.getSelectedText().isEmpty());
        assertTrue(beforeBounds.getMinY() < afterBounds.getMinY());
    }

    @Test
    public void shift_page_up_moves_caret_to_top_of_viewport_and_makes_selection() {
        interact(() -> {
            area.moveTo(5, 0);
            area.requestFollowCaret();
        });
        assertTrue(area.getSelectedText().isEmpty());
        Bounds beforeBounds = area.getCaretBounds().get();

        press(SHIFT).type(PAGE_UP).release(SHIFT);

        Bounds afterBounds = area.getCaretBounds().get();
        assertEquals(0, area.getCaretPosition());
        assertEquals(area.getText(0, 0, 5, 0), area.getSelectedText());
        assertTrue(beforeBounds.getMinY() > afterBounds.getMinY());
    }

    @Test
    public void shift_page_down_moves_caret_to_bottom_of_viewport_and_makes_selection() {
        interact(() -> {
            area.moveTo(0);
            area.requestFollowCaret();
        });
        assertTrue(area.getSelectedText().isEmpty());
        Bounds beforeBounds = area.getCaretBounds().get();

        press(SHIFT).type(PAGE_DOWN).release(SHIFT);

        Bounds afterBounds = area.getCaretBounds().get();
        assertEquals(area.getAbsolutePosition(5, 0), area.getCaretPosition());
        assertEquals(area.getText(0, 0, 5, 0), area.getSelectedText());
        assertTrue(beforeBounds.getMinY() < afterBounds.getMinY());
    }

}

