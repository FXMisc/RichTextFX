package org.fxmisc.richtext.keyboard.navigation;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.END;
import static javafx.scene.input.KeyCode.HOME;
import static javafx.scene.input.KeyCode.SHIFT;
import static javafx.scene.input.KeyCode.SHORTCUT;
import static javafx.scene.input.KeyCode.UP;
import static org.fxmisc.richtext.keyboard.navigation.Utils.entityEnd;
import static org.fxmisc.richtext.keyboard.navigation.Utils.entityStart;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(NestedRunner.class)
public class MultiLineGridlikeTextTests extends InlineCssTextAreaAppTest {

    public final String[] lines = {
            "01 02 03 04 05",
            "11 12 13 14 15",
            "21 22 23 24 25",
            "31 32 33 34 35",
            "41 42 43 44 45"
    };

    private int lineStart(int lineIndex) {
        return entityStart(lineIndex, lines);
    }

    private int lineEnd(int lineIndex) {
        return entityEnd(lineIndex, lines, area);
    }

    String fullText = String.join(" ", lines);

    private void moveCaretTo(int position) {
        area.moveTo(position);
    }

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        area.setWrapText(true);
        area.replaceText(fullText);

        // insures area's text appears exactly as the declaration of `lines`
        stage.setWidth(150);
        area.setStyle(
                "-fx-font-family: monospace;" +
                        "-fx-font-size: 12pt;"
        );
    }

    public class When_No_Modifiers_Pressed {

        @Test
        public void up_moves_caret_to_previous_line() {
            moveCaretTo(lineStart(2));
            assertTrue(area.getSelectedText().isEmpty());

            type(UP);

            assertEquals(lineStart(1), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void down_moves_caret_to_next_line() {
            moveCaretTo(lineStart(1));
            assertTrue(area.getSelectedText().isEmpty());

            type(DOWN);

            assertEquals(lineStart(2), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void home_moves_caret_to_start_of_current_line() {
            moveCaretTo(lineEnd(1));
            assertTrue(area.getSelectedText().isEmpty());

            type(HOME);

            assertEquals(lineStart(1), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void end_moves_caret_to_end_of_current_line() {
            moveCaretTo(lineStart(1));
            assertTrue(area.getSelectedText().isEmpty());

            type(END);

            assertEquals(lineEnd(1), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

    }

    public class When_Shortcut_Is_Pressed {

        @Before
        public void setup() {
            press(SHORTCUT);
        }

        @Test
        public void up_does_not_move_caret() {
            assertTrue(area.getSelectedText().isEmpty());
            moveCaretTo(lineStart(2));

            type(UP);

            assertEquals(lineStart(2), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void down_does_not_move_caret() {
            assertTrue(area.getSelectedText().isEmpty());
            moveCaretTo(lineStart(2));

            type(DOWN);

            assertEquals(lineStart(2), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void home_moves_caret_to_start_of_current_paragraph() {
            moveCaretTo(lineStart(2));
            assertTrue(area.getSelectedText().isEmpty());

            type(HOME);

            assertEquals(0, area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void end_moves_caret_to_end_of_current_paragraph() {
            moveCaretTo(lineStart(1));
            assertTrue(area.getSelectedText().isEmpty());

            type(END);

            assertEquals(area.getLength(), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

    }

    public class When_Shift_Is_Pressed {

        @Before
        public void setup() {
            press(SHIFT);
        }

        @Test
        public void up() {
            moveCaretTo(lineStart(2));
            assertTrue(area.getSelectedText().isEmpty());

            type(UP);

            assertEquals(lineStart(1), area.getCaretPosition());
            assertEquals(lines[1] + " ", area.getSelectedText());
        }

        @Test
        public void down() {
            moveCaretTo(lineStart(1));
            assertTrue(area.getSelectedText().isEmpty());

            type(DOWN);

            assertEquals(lineStart(2), area.getCaretPosition());
            assertEquals(lines[1] + " ", area.getSelectedText());
        }

        @Test
        public void home_selects_up_to_the_start_of_current_line() {
            moveCaretTo(lineEnd(1));
            assertTrue(area.getSelectedText().isEmpty());

            type(HOME);

            assertEquals(lineStart(1), area.getCaretPosition());
            assertEquals(lines[1], area.getSelectedText());
        }

        @Test
        public void end_selects_up_to_the_end_of_current_line() {
            moveCaretTo(lineStart(1));
            assertTrue(area.getSelectedText().isEmpty());

            type(END);

            assertEquals(lineEnd(1), area.getCaretPosition());
            assertEquals(lines[1], area.getSelectedText());
        }

    }

    public class When_Shortcut_And_Shift_Pressed {

        @Before
        public void setup() {
            press(SHORTCUT, SHIFT);
        }

        @Test
        public void up_does_not_move_caret() {
            moveCaretTo(lineStart(2));
            assertTrue(area.getSelectedText().isEmpty());

            type(UP);

            assertEquals(lineStart(2), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void down_does_not_move_caret() {
            moveCaretTo(lineStart(1));
            assertTrue(area.getSelectedText().isEmpty());

            type(DOWN);

            assertEquals(lineStart(1), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void home_selects_up_to_the_start_of_current_paragraph() {
            moveCaretTo(area.getLength());
            assertTrue(area.getSelectedText().isEmpty());

            type(HOME);

            assertEquals(0, area.getCaretPosition());
            assertEquals(area.getText(), area.getSelectedText());
        }

        @Test
        public void end_selects_up_to_the_end_of_current_paragraph() {
            moveCaretTo(0);
            assertTrue(area.getSelectedText().isEmpty());

            type(END);

            assertEquals(area.getLength(), area.getCaretPosition());
            assertEquals(area.getText(), area.getSelectedText());
        }

    }

}
