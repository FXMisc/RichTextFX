package org.fxmisc.richtext.keyboard.navigation;

import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.UP;
import static org.junit.Assert.assertEquals;

public class MultiLineJaggedTextTests extends InlineCssTextAreaAppTest {

    String threeLinesOfText = "Some long amount of text to take up a lot of space in the given area.";

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        stage.setWidth(200);
        area.replaceText(threeLinesOfText);
        area.setWrapText(true);
    }

    private void waitForMultiLineRegistration() throws TimeoutException {
        // When the stage's width changes, TextFlow does not properly handle API calls to a
        //  multi-line paragraph immediately. So, wait until it correctly responds
        //  to the stage width change
        Future<Void> textFlowIsReady = WaitForAsyncUtils.asyncFx(() -> {
            while (area.getParagraphLinesCount(0) != 3) {
                sleep(10);
            }
        });
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, textFlowIsReady);
    }

    @Test
    public void pressing_down_moves_caret_to_next_line() throws TimeoutException {
        waitForMultiLineRegistration();

        area.moveTo(27);

        push(DOWN);

        assertEquals(57, area.getCaretPosition());
    }

    @Test
    public void pressing_up_moves_caret_to_previous_line() throws TimeoutException {
        waitForMultiLineRegistration();

        area.moveTo(66);

        push(UP);

        assertEquals(36, area.getCaretPosition());
    }
}
