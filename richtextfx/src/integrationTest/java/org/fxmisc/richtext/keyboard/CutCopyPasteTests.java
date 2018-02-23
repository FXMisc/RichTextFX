package org.fxmisc.richtext.keyboard;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testfx.framework.junit.ApplicationTest;

import static javafx.scene.input.KeyCode.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assume.assumeFalse;

@RunWith(NestedRunner.class)
public class CutCopyPasteTests extends InlineCssTextAreaAppTest {

    String text = "text";

    String beginning = "Once upon a time, ";
    String middle = "a princess was saved";
    String end = " by a knight.";

    String fullText = beginning + middle + end;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        area.replaceText(fullText);
    }

    public class When_Nothing_Is_Selected {

        @Before
        public void insureSelectionIsEmpty() {
            area.moveTo(beginning.length());
            assertTrue(area.getSelectedText().isEmpty());
        }

        public class Nothing_Is_Stored_In_Clipboard_When_Copy_Via {

            private void runAssert() {
                interact(() -> assertFalse(Clipboard.getSystemClipboard().hasString()));
            }

            @Before
            public void insureClipboardHasNoContent() {
                interact(() -> Clipboard.getSystemClipboard().clear());
            }

            @Test
            public void copy() {
                assumeFalse(USING_AWT_ADAPTER && IS_WINDOWS);

                press(COPY);

                runAssert();
            }

            @Test
            public void shortcut_c() {
                press(SHORTCUT, C);

                runAssert();
            }

            @Test
            public void shortcut_insert() {
                press(SHORTCUT, INSERT);

                runAssert();
            }

        }

        public class Nothing_Is_Removed_In_Area_When_Cut_Via {

            private void runAssert() {
                assertEquals(fullText, area.getText());
            }

            @Test
            public void cut() {
                assumeFalse(USING_AWT_ADAPTER && IS_WINDOWS);

                press(CUT);

                runAssert();
            }

            @Test
            public void shortcut_x() {
                press(SHORTCUT, X);

                runAssert();
            }

            @Test
            public void shift_delete() {
                press(SHIFT, DELETE);

                runAssert();
            }

        }

        public class Text_Is_Inserted_In_Area_When_Paste_Via {

            private void runAssert() {
                assertEquals(beginning + text + middle + end, area.getText());
            }

            @Before
            public void storeTextInClipboard() {
                interact(() -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(text);
                    Clipboard.getSystemClipboard().setContent(content);
                });
            }

            @Test
            public void paste() {
                // this test fails on Linux; Windows is untested
                // so for now, only run on Mac
                // TODO: update if test succeeds on Windows, too
                run_only_on_mac();

                press(PASTE);

                runAssert();
            }

            @Test
            public void shortcut_v() {
                press(SHORTCUT, V);

                runAssert();
            }

            @Test
            public void shift_insert() {
                press(SHIFT, INSERT);

                runAssert();
            }
        }
    }

    public class When_Text_Is_Selected {

        int startMiddle = beginning.length();
        int endMiddle = startMiddle + middle.length();

        @Before
        public void selectMiddleAndClearClipboard() {
            area.selectRange(startMiddle, endMiddle);
            assertEquals(middle, area.getSelectedText());
            interact(() -> Clipboard.getSystemClipboard().clear());
        }

        public class Selection_Is_Stored_In_Clipboard_When_Copy_Via {

            private void runAssert() {
                interact(() -> {
                    assertTrue(Clipboard.getSystemClipboard().hasString());
                    assertEquals(middle, Clipboard.getSystemClipboard().getString());
                });
            }

            @Test
            public void copy() {
                // this test fails on Linux; Windows is untested
                // so for now, only run on Mac
                // TODO: update if test succeeds on Windows, too
                run_only_on_mac();

                press(COPY);

                runAssert();
            }

            @Test
            public void shortcut_c() {
                press(SHORTCUT, C);

                runAssert();
            }

            @Test
            public void shortcut_insert() {
                press(SHORTCUT, INSERT);

                runAssert();
            }

        }

        public class Selection_Is_Removed_And_Stored_In_Clipboard_When_Cut_Via {

            private void runAssert() {
                assertEquals(beginning + end, area.getText());
                interact(() -> {
                    assertTrue(Clipboard.getSystemClipboard().hasString());
                    assertEquals(middle, Clipboard.getSystemClipboard().getString());
                });
            }

            @Test
            public void cut() {
                // this test fails on Linux; Windows is untested
                // so for now, only run on Mac
                // TODO: update if test succeeds on Windows, too
                run_only_on_mac();

                press(CUT);

                runAssert();
            }

            @Test
            public void shortcut_x() {
                press(SHORTCUT, X);

                runAssert();
            }

            @Test
            public void shift_delete() {
                press(SHIFT, DELETE);

                runAssert();
            }

        }

        public class Selection_Is_Replaced_In_Area_When_Paste_Via {

            @Before
            public void storeTextInClipboard() {
                interact(() -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(text);
                    Clipboard.getSystemClipboard().setContent(content);
                });
            }

            private void runAssert() {
                assertEquals(beginning + text + end, area.getText());
            }

            @Test
            public void paste() {
                // this test fails on Linux; Windows is untested
                // so for now, only run on Mac
                // TODO: update if test succeeds on Windows, too
                run_only_on_mac();

                press(PASTE);

                runAssert();
            }

            @Test
            public void shortcut_v() {
                press(SHORTCUT, V);

                runAssert();
            }

            @Test
            public void shift_insert() {
                press(SHIFT, INSERT);

                runAssert();
            }

        }
    }

    public class MiscellaneousCases extends ApplicationTest {

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
                interact(() -> area.selectRange(2, 4));
            }

            @Test
            public void copying_and_pasting_should_not_throw_exception() {
                interact(area::copy);
                interact(area::paste);
            }
        }
    }

}