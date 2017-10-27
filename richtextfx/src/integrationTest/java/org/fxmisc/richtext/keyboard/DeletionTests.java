package org.fxmisc.richtext.keyboard;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.SHORTCUT;
import static org.fxmisc.richtext.NavigationActions.SelectionPolicy.CLEAR;
import static org.junit.Assert.assertEquals;

@RunWith(NestedRunner.class)
public class DeletionTests {

    String text = "text";
    String text2 = join(text, text);
    String text3 = join(text2, text);

    private String join(String... strings) {
        return String.join(" ", strings);
    }

    private String withoutLastChar(String s) {
        return s.substring(0, s.length() - 1);
    }

    private String withoutFirstChar(String s) {
        return s.substring(1);
    }

    public class When_Shortcut_Is_Down extends InlineCssTextAreaAppTest {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);
            area.replaceText(text3);

            press(SHORTCUT);
        }

        @Test
        public void pressing_delete_removes_next_word_and_space() {
            area.moveTo(0);
            int pos = area.getCaretPosition();

            press(DELETE);

            assertEquals(text2, area.getText());
            assertEquals(pos, area.getCaretPosition());
        }

        @Test
        public void pressing_backspace_removes_previous_word_and_space() {
            area.end(CLEAR);
            int pos = area.getCaretPosition();

            press(BACK_SPACE);

            assertEquals(text2, area.getText());
            assertEquals(pos - text.length() - 1, area.getCaretPosition());
        }
    }

    public class When_No_Modifiers extends InlineCssTextAreaAppTest  {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);
            area.replaceText(text);
        }

        @Test
        public void pressing_delete_removes_next_char() {
            area.moveTo(0);
            int pos = area.getCaretPosition();

            push(DELETE);

            assertEquals(withoutFirstChar(text), area.getText());
            assertEquals(pos, area.getCaretPosition());
        }

        @Test
        public void pressing_backspace_removes_previous_char() {
            area.end(CLEAR);
            int pos = area.getCaretPosition();

            push(BACK_SPACE);

            assertEquals(withoutLastChar(text), area.getText());
            assertEquals(pos - 1, area.getCaretPosition());
        }

    }

    // miscellaneous cases

    public class When_Area_Ends_With_Empty_Line extends InlineCssTextAreaAppTest {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);
            area.replaceText(0, 0, "abc\n");
        }

        public class And_All_Text_Is_Selected {

            @Before
            public void selectAllText() {
                interact(() -> area.selectAll());
            }


            @Test
            public void pressing_delete_should_not_throw_exception() {
                push(DELETE);
            }

            @Test
            public void pressing_backspace_should_not_throw_exceptions() {
                push(BACK_SPACE);
            }

        }
    }
}
