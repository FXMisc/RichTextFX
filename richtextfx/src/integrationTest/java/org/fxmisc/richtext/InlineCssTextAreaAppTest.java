package org.fxmisc.richtext;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.testfx.api.FxRobotInterface;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.service.query.PointQuery;

/**
 * TestFX tests should subclass this if it needs to run tests on a simple area. Any view-related API needs to be
 * wrapped in a {@link #interact(Runnable)} call, but model API does not need to be wrapped in it.
 */
public class InlineCssTextAreaAppTest extends ApplicationTest {

    static {
        String osName = System.getProperty("os.name").toLowerCase();

        WINDOWS_OS = osName.contains("win");
    }

    private static final boolean WINDOWS_OS;
    public static boolean isWindows() { return WINDOWS_OS; }

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

    public final FxRobotInterface clickOnFirstLine(MouseButton... buttons) {
        return moveTo(firstLineOfArea()).clickOn(buttons);
    }

    public final FxRobotInterface leftClickOnFirstLine() {
        return clickOnFirstLine(MouseButton.PRIMARY);
    }

    public final FxRobotInterface doubleClickOnFirstLine() {
        return leftClickOnFirstLine().clickOn(MouseButton.PRIMARY);
    }

    public final FxRobotInterface tripleClickOnFirstLine() {
        return doubleClickOnFirstLine().clickOn(MouseButton.PRIMARY);
    }

    public final FxRobotInterface rightClickOnFirstLine() {
        return clickOnFirstLine(MouseButton.SECONDARY);
    }
}
