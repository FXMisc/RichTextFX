package org.fxmisc.richtext.api.caret;

import javafx.stage.Stage;
import org.fxmisc.richtext.CaretNode;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PositionTests extends InlineCssTextAreaAppTest {

    CaretNode caret;
    String text = "text";

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        area.replaceText(text);
        caret = new CaretNode("extra caret", area);
        area.addCaret(caret);
    }

    @Test
    public void position_is_correct_when_change_occurs_before_position() {
        interact(() -> {
            caret.moveToAreaEnd();
            int pos = caret.getPosition();

            String append = "some";

            // add test
            area.insertText(0, append);
            assertEquals(pos + append.length(), caret.getPosition());

            // delete test
            area.deleteText(0, append.length());
            assertEquals(pos, caret.getPosition());
        });
    }

    @Test
    public void position_is_correct_when_change_occurs_before_position_and_deletes_carets_position() {
        interact(() -> {
            caret.moveTo(text.length() - 1);

            area.appendText("append");
            area.deleteText(0, text.length());
            assertEquals(0, caret.getPosition());
        });
    }

    @Test
    public void position_is_correct_when_change_occurs_at_position() {
        interact(() -> {
            caret.moveToAreaEnd();
            int pos = caret.getPosition();

            String append = "some";
            // add test
            area.appendText(append);
            assertEquals(pos + append.length(), caret.getPosition());

            // reset
            caret.moveTo(pos);

            // delete test
            area.deleteText(pos, area.getLength());
            assertEquals(pos, caret.getPosition());
        });
    }

    @Test
    public void position_is_correct_when_change_occurs_after_position() {
        interact(() -> {
            caret.moveTo(0);

            // add test
            String append = "some";
            area.appendText(append);
            assertEquals(0, caret.getPosition());

            // delete test
            int length = area.getLength();
            area.deleteText(length - append.length(), length);
            assertEquals(0, caret.getPosition());
        });
    }

}