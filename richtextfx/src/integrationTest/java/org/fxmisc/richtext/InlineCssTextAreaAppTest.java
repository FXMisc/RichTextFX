package org.fxmisc.richtext;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.testfx.api.FxRobotInterface;
import org.testfx.service.query.PointQuery;

/**
 * TestFX tests should subclass this if it needs to run tests on a simple area. Any view-related API needs to be
 * wrapped in a {@link #interact(Runnable)} call, but model API does not need to be wrapped in it.
 */
public class InlineCssTextAreaAppTest extends RichTextFXTestBase {

    public Stage stage;
    public Scene scene;
    public InlineCssTextArea area;

    @Override
    public void start(Stage stage) throws Exception {
        area = new InlineCssTextArea();
        scene = new Scene(area);
        this.stage = stage;

        stage.setAlwaysOnTop(true);
        stage.setScene(scene);
        // Size the stage before showing it. Don't resize it right after show() in a subclass's start():
        // on Linux under a window manager, JavaFX 11 then sends the X server a stale size for the
        // dimension that was not changed (e.g. setHeight(100) yields a 1x100 window), while JavaFX still
        // thinks the window is full size. Mouse clicks then miss the window. Override the methods below.
        stage.setWidth(getStageWidth());
        stage.setHeight(getStageHeight());
        stage.show();

        // so tests don't need to do this themselves
        area.requestFocus();
    }

    /** The width the stage is given before it is shown. */
    protected double getStageWidth() {
        return 400;
    }

    /** The height the stage is given before it is shown. */
    protected double getStageHeight() {
        return 400;
    }

    public final PointQuery position(Pos pos, double xOffset, double yOffset) {
        return position(area, pos, xOffset, yOffset);
    }

    public final PointQuery firstLineOfArea() {
        return position(Pos.TOP_LEFT, 5, 5);
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
