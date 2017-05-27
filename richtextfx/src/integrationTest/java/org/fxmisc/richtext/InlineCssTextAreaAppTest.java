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

import static org.junit.Assume.assumeTrue;

/**
 * TestFX tests should subclass this if it needs to run tests on a simple area. Any view-related API needs to be
 * wrapped in a {@link #interact(Runnable)} call, but model API does not need to be wrapped in it.
 */
public class InlineCssTextAreaAppTest extends ApplicationTest {

    static {
        String osName = System.getProperty("os.name").toLowerCase();

        isWindows = osName.startsWith("win");
        isMac = osName.startsWith("mac");
        isLinux = osName.startsWith("linux");
    }

    public static final boolean isWindows;
    public static final boolean isMac;
    public static final boolean isLinux;

    public Stage stage;
    public Scene scene;
    public InlineCssTextArea area;
    public ContextMenu menu;

    @Override
    public void start(Stage stage) throws Exception {
        area = new InlineCssTextArea();
        scene = new Scene(area);
        this.stage = stage;

        stage.setScene(scene);
        stage.setWidth(400);
        stage.setHeight(400);
        stage.show();

        menu = new ContextMenu(new MenuItem("A menu item"));
        area.setContextMenu(menu);
        // offset needs to be 5+ to prevent test failures
        area.setContextMenuXOffset(30);
        area.setContextMenuYOffset(30);

        // so tests don't need to do this themselves
        area.requestFocus();
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

    /**
     * If not on Windows environment, calling this in @Before method will skip the entire test suite whereas calling
     * this in @Test will skip just that test method
     */
    public final void run_only_on_windows() {
        assumeTrue(isWindows);
    }

    /**
     * If not on Linux environment, calling this in @Before method will skip the entire test suite whereas calling
     * this in @Test will skip just that test method
     */
    public final void run_only_on_linux() {
        assumeTrue(isLinux);
    }

    /**
     * If not on Mac environment, calling this in @Before method will skip the entire test suite whereas calling
     * this in @Test will skip just that test method
     */
    public final void run_only_on_mac() {
        assumeTrue(isMac);
    }

    /**
     * If on Windows environment, calling this in @Before method will skip the entire test suite whereas calling
     * this in @Test will skip just that test method
     */
    public final void skip_if_on_windows() {
        assumeTrue(!isWindows);
    }

    /**
     * If on Linux environment, calling this in @Before method will skip the entire test suite whereas calling
     * this in @Test will skip just that test method
     */
    public final void skip_if_on_linux() {
        assumeTrue(!isLinux);
    }

    /**
     * If on Mac environment, calling this in @Before method will skip the entire test suite whereas calling
     * this in @Test will skip just that test method
     */
    public final void skip_if_on_mac() {
        assumeTrue(!isMac);
    }
}
