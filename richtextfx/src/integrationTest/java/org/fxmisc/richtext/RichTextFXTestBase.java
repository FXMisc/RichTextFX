package org.fxmisc.richtext;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.service.query.PointQuery;

import static org.junit.Assume.assumeTrue;

/**
 * Provides useful static fields and helper methods for RichTextFX integration tests.
 *
 * <ul>
 *     <li>
 *         Helps determine which OS is currently running the test and whether to run/skip a test on that OS
 *     </li>
 *     <li>
 *         Getting the position o
 *     </li>
 * </ul>
 */
public abstract class RichTextFXTestBase extends ApplicationTest {

    static {
        String robotProperty = System.getProperty("testfx.robot");
        boolean usingGlass = robotProperty != null && robotProperty.equals("glass");

        USING_GLASS_ADAPTER = usingGlass;
        USING_AWT_ADAPTER = !usingGlass;

        String osName = System.getProperty("os.name").toLowerCase();

        IS_WINDOWS = osName.startsWith("win");
        IS_MAC = osName.startsWith("mac");
        IS_LINUX = osName.startsWith("linux");
    }

    /* *********************************************** *
     * TestFX--Related
     * *********************************************** */

    public static final boolean USING_AWT_ADAPTER;
    public static final boolean USING_GLASS_ADAPTER;

    public final void skip_if_using_glass_robot() {
        assumeTrue(USING_AWT_ADAPTER);
    }

    public final void skip_if_using_awt_robot() {
        assumeTrue(USING_GLASS_ADAPTER);
    }

    /* *********************************************** *
     * OS-RELATED
     * *********************************************** */

    public static final boolean IS_WINDOWS;
    public static final boolean IS_MAC;
    public static final boolean IS_LINUX;

    /**
     * If not on Windows environment, calling this in @Before method will skip the entire test suite whereas calling
     * this in @Test will skip just that test method
     */
    public final void run_only_on_windows() {
        assumeTrue(IS_WINDOWS);
    }

    /**
     * If not on Linux environment, calling this in @Before method will skip the entire test suite whereas calling
     * this in @Test will skip just that test method
     */
    public final void run_only_on_linux() {
        assumeTrue(IS_LINUX);
    }

    /**
     * If not on Mac environment, calling this in @Before method will skip the entire test suite whereas calling
     * this in @Test will skip just that test method
     */
    public final void run_only_on_mac() {
        assumeTrue(IS_MAC);
    }

    /**
     * If on Windows environment, calling this in @Before method will skip the entire test suite whereas calling
     * this in @Test will skip just that test method
     */
    public final void skip_if_on_windows() {
        assumeTrue(!IS_WINDOWS);
    }

    /**
     * If on Linux environment, calling this in @Before method will skip the entire test suite whereas calling
     * this in @Test will skip just that test method
     */
    public final void skip_if_on_linux() {
        assumeTrue(!IS_LINUX);
    }

    /**
     * If on Mac environment, calling this in @Before method will skip the entire test suite whereas calling
     * this in @Test will skip just that test method
     */
    public final void skip_if_on_mac() {
        assumeTrue(!IS_MAC);
    }

    /* *********************************************** *
     * Position-Related
     * *********************************************** */

    /**
     * Returns a specific position in the scene, starting at {@code pos} and offsetting from that place by
     * {@code xOffset} and {@code yOffset}
     */
    public final PointQuery position(Scene scene, Pos pos, double xOffset, double yOffset) {
        return point(scene).atPosition(pos).atOffset(xOffset, yOffset);
    }

    /**
     * Returns a specific position in the window, starting at {@code pos} and offsetting from that place by
     * {@code xOffset} and {@code yOffset}
     */
    public final PointQuery position(Window window, Pos pos, double xOffset, double yOffset) {
        return point(window).atPosition(pos).atOffset(xOffset, yOffset);
    }

    /**
     * Returns a specific position in the node, starting at {@code pos} and offsetting from that place by
     * {@code xOffset} and {@code yOffset}
     */
    public final PointQuery position(Node node, Pos pos, double xOffset, double yOffset) {
        return point(node).atPosition(pos).atOffset(xOffset, yOffset);
    }
}
