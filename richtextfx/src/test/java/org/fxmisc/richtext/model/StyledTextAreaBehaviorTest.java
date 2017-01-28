package org.fxmisc.richtext.model;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testfx.framework.junit.ApplicationTest;

@RunWith(NestedRunner.class)
public class StyledTextAreaBehaviorTest {

    static {
        String osName = System.getProperty("os.name").toLowerCase();

        WINDOWS_OS = osName.contains("win");
    }

    private static final boolean WINDOWS_OS;

    public class ContextMenuTests extends ApplicationTest {

        public InlineCssTextArea area = new InlineCssTextArea();

        @Override
        public void start(Stage stage) throws Exception {
            area.setContextMenu(new ContextMenu(new MenuItem("A Menu Item")));
            // offset needs to be 5 to prevent test failures
            area.setContextMenuXOffset(5);
            area.setContextMenuYOffset(5);

            VirtualizedScrollPane<InlineCssTextArea> pane = new VirtualizedScrollPane<>(area);
            stage.setScene(new Scene(pane, 500, 500));
            stage.show();
        }

        @Test
        public void clickingSecondaryShowsContextMenu() {
            // when
            rightClickOn(area);

            // then
            area.getContextMenu().isShowing();
        }

        @Test
        public void pressingSecondaryShowsContextMenu() {
            // when
            moveTo(area);
            press(MouseButton.SECONDARY);

            // then
            area.getContextMenu().isShowing();
        }

        @Test
        public void pressingPrimaryMouseButtonHidesContextMenu() {
            // given menu is showing
            rightClickOn(area);

            press(MouseButton.PRIMARY);
            assert !area.getContextMenu().isShowing();
        }

        @Test
        public void pressingMiddleMouseButtonHidesContextMenu() {
            // given menu is showing
            rightClickOn(area);

            press(MouseButton.MIDDLE);
            assert !area.getContextMenu().isShowing();
        }

        @Test
        public void requestingContextMenuViaKeyboardWorksOnWindows() {
            if (WINDOWS_OS) {
                clickOn(area);
                press(KeyCode.CONTEXT_MENU);

                assert area.getContextMenu().isShowing();
            }
        }

    }

}
