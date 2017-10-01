package org.fxmisc.richtext.keyboard.navigation;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static javafx.scene.input.KeyCode.A;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.SHIFT;
import static javafx.scene.input.KeyCode.SHORTCUT;
import static org.fxmisc.richtext.keyboard.navigation.Utils.entityEnd;
import static org.fxmisc.richtext.keyboard.navigation.Utils.entityStart;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(NestedRunner.class)
public class SingleLineTests extends InlineCssTextAreaAppTest {

    String[] words = { "the", "cat", "can", "walk" };
    String fullText = String.join(" ", words);

    private int wordStart(int wordIndex) {
        return entityStart(wordIndex, words);
    }

    private int wordEnd(int wordIndex) {
        return entityEnd(wordIndex, words, area);
    }

    private void moveCaretTo(int position) {
        area.moveTo(position);
    }

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        stage.setWidth(300);
        area.replaceText(fullText);
    }

    public class When_No_Modifiers_Pressed {

        @Test
        public void left_moves_caret_one_position() {
            moveCaretTo(wordStart(1));
            assertTrue(area.getSelectedText().isEmpty());

            type(LEFT);

            assertEquals(wordEnd(0), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void right_moves_caret_one_position() {
            moveCaretTo(wordStart(1));
            assertTrue(area.getSelectedText().isEmpty());

            type(RIGHT);

            assertEquals(wordStart(1) + 1, area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

    }

    public class When_Shortcut_Is_Pressed {

        @Before
        public void setup() {
            press(SHORTCUT);
        }

        @Test
        public void left_once_moves_caret_to_left_boundary_of_current_word() {
            moveCaretTo(wordEnd(3));
            assertTrue(area.getSelectedText().isEmpty());

            // first left goes to boundary of current word
            type(LEFT);

            assertEquals(wordStart(3), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void left_twice_moves_caret_to_left_boundary_of_previous_word() {
            moveCaretTo(wordEnd(3));
            assertTrue(area.getSelectedText().isEmpty());

            type(LEFT).type(LEFT);

            assertEquals(wordStart(2), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void right_once_moves_caret_to_right_boundary_of_current_word() {
            moveCaretTo(wordStart(0));
            assertTrue(area.getSelectedText().isEmpty());

            // first right goes to boundary of current word
            type(RIGHT);

            assertEquals(wordEnd(0), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void right_twice_moves_caret_to_right_boundary_of_next_word() {
            moveCaretTo(wordStart(0));
            assertTrue(area.getSelectedText().isEmpty());

            type(RIGHT).type(RIGHT);

            assertEquals(wordEnd(1), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void a_selects_all() {
            assertTrue(area.getSelectedText().isEmpty());

            type(A);

            assertEquals(area.getText(), area.getSelectedText());
        }

    }

    public class When_Shift_Is_Pressed {

        @Before
        public void setup() {
            press(SHIFT);
        }

        @Test
        public void left_selects_previous_character() {
            moveCaretTo(wordStart(1));
            assertTrue(area.getSelectedText().isEmpty());

            type(LEFT);

            assertEquals(wordEnd(0), area.getCaretPosition());
            assertEquals(" ", area.getSelectedText());
        }

        @Test
        public void right_selects_next_character() {
            moveCaretTo(wordEnd(0));
            assertTrue(area.getSelectedText().isEmpty());

            type(RIGHT);

            assertEquals(wordStart(1), area.getCaretPosition());
            assertEquals(" ", area.getSelectedText());
        }

    }

    public class When_Shortcut_And_Shift_Pressed {

        @Before
        public void setup() {
            press(SHORTCUT, SHIFT);
        }

        @Test
        public void left_once_selects_up_to_left_boundary_of_current_word() {
            moveCaretTo(wordEnd(3));
            assertTrue(area.getSelectedText().isEmpty());

            // first left goes to boundary of current word
            type(LEFT);

            assertEquals(wordStart(3), area.getCaretPosition());
            assertEquals(words[3], area.getSelectedText());
        }

        @Test
        public void left_twice_selects_up_to_start_boundary_of_previous_word() {
            moveCaretTo(wordEnd(3));
            assertTrue(area.getSelectedText().isEmpty());

            type(LEFT).type(LEFT);

            assertEquals(wordStart(2), area.getCaretPosition());
            assertEquals(words[2] + " " + words[3], area.getSelectedText());
        }

        @Test
        public void right_once_selects_up_to_right_boundary_of_current_word() {
            moveCaretTo(wordStart(0));
            assertTrue(area.getSelectedText().isEmpty());

            type(RIGHT);

            assertEquals(wordEnd(0), area.getCaretPosition());
            assertEquals(words[0], area.getSelectedText());
        }

        @Test
        public void right_twice_selects_up_to_end_boundary_of_next_word() {
            moveCaretTo(wordStart(0));
            assertTrue(area.getSelectedText().isEmpty());

            type(RIGHT).type(RIGHT);

            assertEquals(wordEnd(1), area.getCaretPosition());
            assertEquals(words[0] + " " + words[1], area.getSelectedText());
        }

    }

}