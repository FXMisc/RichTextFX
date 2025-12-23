package org.fxmisc.richtext.keyboard.navigation;

import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.UP;
import static org.junit.Assert.assertEquals;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class MultiLineJaggedTextTests extends InlineCssTextAreaAppTest {
    String threeLinesOfText = "Some long amount of text to take up a lot of space in the given area.";

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        stage.setWidth(200);
        area.replaceText(threeLinesOfText);
        area.setWrapText(true);
    }

    @Test
    public void pressing_down_moves_caret_to_next_line() {
        area.moveTo(0);
        assertEquals(0, area.getCaretSelectionBind().getLineIndex().getAsInt());

        push(DOWN);

        assertEquals(1, area.getCaretSelectionBind().getLineIndex().getAsInt());
    }

    @Test
    public void pressing_up_moves_caret_to_previous_line() {
        area.moveTo(area.getLength());
        int lastLineIndex = area.getParagraphLinesCount(0) - 1;
        assertEquals(lastLineIndex, area.getCaretSelectionBind().getLineIndex().getAsInt());

        push(UP);

        assertEquals(lastLineIndex - 1, area.getCaretSelectionBind().getLineIndex().getAsInt());
    }
}
