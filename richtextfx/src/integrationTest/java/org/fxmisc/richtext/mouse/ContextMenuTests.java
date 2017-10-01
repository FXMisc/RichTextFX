package org.fxmisc.richtext.mouse;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.After;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class ContextMenuTests extends InlineCssTextAreaAppTest {

    private ContextMenu menu;

    // offset needs to be 5+ to prevent test failures
    private double offset = 30;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        menu = new ContextMenu(new MenuItem("A menu item"));
        area.setContextMenu(menu);
        area.setContextMenuXOffset(offset);
        area.setContextMenuYOffset(offset);
    }

    @After
    public void cleanup() {
        interact(menu::hide);
    }

    @Test
    public void clicking_secondary_shows_context_menu() {
        // Linux passes; Mac fails; Windows untested
        //  so for now, only run on Linux
        // TODO: See if tests pass on Windows
        run_only_on_linux();

        // when
        rightClickOnFirstLine();

        // then
        assertTrue(area.getContextMenu().isShowing());
    }

    @Test
    public void pressing_secondary_shows_context_menu() {
        // Linux passes; Mac fails; Windows untested
        //  so for now, only run on Linux
        // TODO: See if tests pass on Windows
        run_only_on_linux();

        // when
        moveTo(firstLineOfArea()).press(MouseButton.SECONDARY);

        // then
        assertTrue(area.getContextMenu().isShowing());
    }

    @Test
    public void pressing_primary_mouse_button_hides_context_menu() {
        // given menu is showing
        showContextMenuAt();

        moveTo(firstLineOfArea()).press(MouseButton.PRIMARY);

        assertFalse(area.getContextMenu().isShowing());
    }

    @Test
    public void pressing_middle_mouse_button_hides_context_menu() {
        // given menu is showing
        showContextMenuAt();

        moveTo(firstLineOfArea()).press(MouseButton.MIDDLE);

        assertFalse(area.getContextMenu().isShowing());
    }

    @Test
    public void requesting_context_nenu_via_keyboard_works_on_windows() {
        run_only_on_windows();

        leftClickOnFirstLine();
        push(KeyCode.CONTEXT_MENU);

        assertTrue(area.getContextMenu().isShowing());
    }

    private void showContextMenuAt() {
        Point2D screenPoint = position(Pos.TOP_LEFT, offset, offset).query();
        interact(() -> area.getContextMenu().show(area, screenPoint.getX(), screenPoint.getY()));
    }

}