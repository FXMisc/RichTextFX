package org.fxmisc.richtext.api;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testfx.framework.junit.ApplicationTest;

import static javafx.scene.input.KeyCode.*;

@RunWith(NestedRunner.class)
public class ClipboardTests {

    public class CopyTests extends ApplicationTest {

        CodeArea area;

        @Override
        public void start(Stage primaryStage) throws Exception {
            area = new CodeArea("abc\ndef\nghi");
            VirtualizedScrollPane<CodeArea> vsPane = new VirtualizedScrollPane<>(area);

            Scene scene = new Scene(vsPane, 400, 400);
            primaryStage.setScene(scene);
            primaryStage.show();
        }


        public class When_User_Makes_Selection_Ending_In_Newline_Character {

            @Before
            public void setup() {
                area.selectRange(2, 4);
            }

            @Test
            public void copying_and_pasting_should_not_throw_exception() {
                push(CONTROL, C);

                push(CONTROL, V);
            }
        }
    }

}
