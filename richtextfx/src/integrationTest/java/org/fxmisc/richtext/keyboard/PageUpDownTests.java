package org.fxmisc.richtext.keyboard;

import javafx.geometry.Bounds;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Ignore;
import org.junit.Test;

import static javafx.scene.input.KeyCode.PAGE_DOWN;
import static javafx.scene.input.KeyCode.PAGE_UP;
import static javafx.scene.input.KeyCode.SHIFT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PageUpDownTests extends InlineCssTextAreaAppTest {

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        // allow 6 lines to be displayed
        stage.setHeight(90);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(i).append("\n");
        }
        sb.append(9);
        area.replaceText(sb.toString());
    }

    @Test
    public void testPageUp() {
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

    @Ignore("doesn't work despite 'testShiftPageDown' working fine using the same code")
    @Test
    public void testPageDown() throws Exception {
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
    public void testShiftPageUp() {
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
    public void testShiftPageDown() {
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

