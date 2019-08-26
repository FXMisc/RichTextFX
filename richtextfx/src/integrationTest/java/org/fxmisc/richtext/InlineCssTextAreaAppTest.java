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
        stage.setWidth(400);
        stage.setHeight(400);
        stage.show();

        // so tests don't need to do this themselves
        area.requestFocus();
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
