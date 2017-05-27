package org.fxmisc.richtext.keyboard;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.SHORTCUT;
import static org.fxmisc.richtext.model.NavigationActions.SelectionPolicy.CLEAR;
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

    public class WhenShortcutIsDown extends InlineCssTextAreaAppTest {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);
            area.replaceText(text3);

            press(SHORTCUT);
        }

        @Test
        public void pressingDeleteRemovesNextWordAndSpace() {
            area.moveTo(0);
            int pos = area.getCaretPosition();

            press(DELETE);

            assertEquals(text2, area.getText());
            assertEquals(pos, area.getCaretPosition());
        }

        @Test
        public void pressingBackspaceRemovesPreviousWordAndSpace() {
            area.end(CLEAR);
            int pos = area.getCaretPosition();

            press(BACK_SPACE);

            assertEquals(text2, area.getText());
            assertEquals(pos - text.length() - 1, area.getCaretPosition());
        }
    }

    public class WhenModifiersAreNotDown extends InlineCssTextAreaAppTest  {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);
            area.replaceText(text);
        }

        @Test
        public void pressingDeleteRemovesNextChar() {
            area.moveTo(0);
            int pos = area.getCaretPosition();

            push(DELETE);

            assertEquals(withoutFirstChar(text), area.getText());
            assertEquals(pos, area.getCaretPosition());
        }

        @Test
        public void pressingBackspaceRemovesPreviousChar() {
            area.end(CLEAR);
            int pos = area.getCaretPosition();

            push(BACK_SPACE);

            assertEquals(withoutLastChar(text), area.getText());
            assertEquals(pos - 1, area.getCaretPosition());
        }

    }
}
