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

        }
    }
}
