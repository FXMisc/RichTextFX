package org.fxmisc.richtext.api;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.RichTextFXTestBase;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;
import org.fxmisc.richtext.util.UndoUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testfx.framework.junit.ApplicationTest;

import static org.junit.Assert.assertEquals;

@RunWith(NestedRunner.class)
public class UndoManagerTests {

    public class UsingInlineCssTextArea extends InlineCssTextAreaAppTest {

        @Test
        public void incoming_change_is_not_merged_after_period_of_user_inactivity() {
            String text1 = "text1";
            String text2 = "text2";

            long periodOfUserInactivity = UndoUtils.DEFAULT_PREVENT_MERGE_DELAY.toMillis() + 300L;

            write(text1);
            sleep(periodOfUserInactivity);
            write(text2);

            interact(area::undo);
            assertEquals(text1, area.getText());

            interact(area::undo);
            assertEquals("", area.getText());
        }

        @Test
        public void testUndoWithWinNewlines() {
            String text1 = "abc\r\ndef";
            String text2 = "A\r\nB\r\nC";

            interact(() -> {
                area.replaceText(text1);
                area.getUndoManager().forgetHistory();
                area.insertText(0, text2);
                assertEquals("A\nB\nCabc\ndef", area.getText());

                area.undo();
                assertEquals("abc\ndef", area.getText());
            });
        }

    }

    public class UsingStyledTextArea extends RichTextFXTestBase {

        @Override
        public void start(Stage stage) throws Exception {
            stage.setScene(new Scene(new Label("Ignore me..."), 400, 400));
            stage.show();
        }

        @Test
        public void testForBug216() {
            interact(() -> {
                // set up area with some styled text content
                boolean initialStyle = false;
                StyledTextArea<String, Boolean> area = new StyledTextArea<>(
                        "", (t, s) -> {},
                        initialStyle, (t, s) -> {},
                        new SimpleEditableStyledDocument<>("", initialStyle), true
                );
                area.replaceText("testtest");
                area.setStyle(0, 8, true);

                // add a space styled by initialStyle
                area.setUseInitialStyleForInsertion(true);
                area.insertText(4, " ");

                // add another space
                area.insertText(5, " ");

                // testing that undo/redo don't throw an exception
                area.undo();
                area.redo();
            });
        }

    }
}
