package org.fxmisc.richtext.model;


import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextArea;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.service.query.PointQuery;

/**
 * TestFX tests should subclass this if it needs to run tests on a simple area. Any view-related API needs to be
 * wrapped in a {@link #interact(Runnable)} call, but model API does not need to be wrapped in it.
 */
public class InlineCssTextAreaAppTest extends ApplicationTest {


    public InlineCssTextArea area;
    public ContextMenu menu;

    @Override
    public void start(Stage stage) throws Exception {
        area = new InlineCssTextArea();
        menu = new ContextMenu(new MenuItem("A menu item"));
        area.setContextMenu(menu);

        // offset needs to be 5 to prevent test failures
        area.setContextMenuXOffset(5);
        area.setContextMenuYOffset(5);

        stage.setScene(new Scene(area, 400, 400));
        stage.show();
    }

    public final PointQuery firstLineOfArea() {
        return point(area).atPosition(Pos.TOP_LEFT).atOffset(5, 5);
    }

    public final FxRobot clickOnFirstLine(MouseButton... buttons) {
        return moveTo(firstLineOfArea()).clickOn(buttons);
    }

    public final FxRobot leftClickOnFirstLine() {
        return clickOnFirstLine(MouseButton.PRIMARY);
    }

    public final FxRobot doubleClickOnFirstLine() {
        return leftClickOnFirstLine().clickOn(MouseButton.PRIMARY);
    }

    public final FxRobot tripleClickOnFirstLine() {
        return doubleClickOnFirstLine().clickOn(MouseButton.PRIMARY);
    }

    public final FxRobot rightClickOnFirstLine() {
        return clickOnFirstLine(MouseButton.SECONDARY);
    }
}
