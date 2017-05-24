package org.fxmisc.richtext.mouse;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class ContextMenuTests extends InlineCssTextAreaAppTest {

    @Test
    public void clickingSecondaryShowsContextMenu() {
        // when
        rightClickOnFirstLine();

        // then
        assertTrue(area.getContextMenu().isShowing());
    }

    @Test
    public void pressingSecondaryShowsContextMenu() {
        // when
        moveTo(firstLineOfArea()).press(MouseButton.SECONDARY);

        // then
        assertTrue(area.getContextMenu().isShowing());
    }

    @Test
    public void pressingPrimaryMouseButtonHidesContextMenu() {
        // given menu is showing
        showContextMenuAt(Pos.TOP_LEFT, 30);

        moveTo(firstLineOfArea()).press(MouseButton.PRIMARY);
        assertFalse(area.getContextMenu().isShowing());
    }

    @Test
    public void pressingMiddleMouseButtonHidesContextMenu() {
        // given menu is showing
        showContextMenuAt(Pos.TOP_LEFT, 30);

        moveTo(firstLineOfArea()).press(MouseButton.MIDDLE);
        assertFalse(area.getContextMenu().isShowing());
    }

    @Ignore // push(CONTEXT_MENU) does not create a ContextMenuEvent properly, causing test to fail
    @Test
    public void requestingContextMenuViaKeyboardWorksOnWindows() {
        if (isWindows()) {
            leftClickOnFirstLine();
            push(KeyCode.CONTEXT_MENU);

            assertTrue(area.getContextMenu().isShowing());
        }
    }

    private void showContextMenuAt(Pos posInArea, double offset) {
        Point2D screenPoint = point(area).atPosition(posInArea).atOffset(offset, offset).query();
        interact(() -> area.getContextMenu().show(area, screenPoint.getX(), screenPoint.getY()));
    }

}