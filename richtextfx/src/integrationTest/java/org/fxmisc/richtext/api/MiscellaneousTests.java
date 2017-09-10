package org.fxmisc.richtext.api;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static javafx.scene.input.KeyCode.DELETE;

@RunWith(NestedRunner.class)
public class MiscellaneousTests {

    public class WhenAreaEndsWithEmptyLine extends InlineCssTextAreaAppTest {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);
            area.replaceText(0, 0, "abc\n");
        }

        public class AndAllTextIsSelected {

            @Before
            public void selectAllText() {
                interact(() -> area.selectAll());
            }


            @Test
            public void pressingDeleteShouldNotThrowException() {
                push(DELETE);
            }

        }
    }
}
