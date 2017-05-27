package org.fxmisc.richtext.keyboard;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static javafx.scene.input.KeyCode.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

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

    public class WhenNothingIsSelected {

        @Before
        public void insureSelectionIsEmpty() {
            area.moveTo(beginning.length());
            assertTrue(area.getSelectedText().isEmpty());
        }

        public class NothingIsStoredInClipboardWhenCopyVia {

            @Before
            public void insureClipboardHasNoContent() {
                interact(() -> Clipboard.getSystemClipboard().clear());
            }

            @After
            public void assertAndCleanup() {
                interact(() -> assertFalse(Clipboard.getSystemClipboard().hasString()));

                // cleanup
                release(COPY, SHORTCUT, C, INSERT);
            }

            @Test public void copy()            { press(COPY); }
            @Test public void shortcut_C()      { press(SHORTCUT, C); }
            @Test public void shortcut_Insert() { press(SHORTCUT, INSERT); }

        }

        public class NothingIsRemovedInAreaWhenCutVia {

            @After
            public void assertAndCleanup() {
                assertEquals(fullText, area.getText());

                release(CUT, SHIFT, X, DELETE);
            }

            @Test public void cut()          { press(CUT); }
            @Test public void shortcut_X()   { press(SHORTCUT, X); }
            @Test public void shift_Delete() { press(SHIFT, DELETE); }

        }

        public class TextIsInsertedInAreaWhenPasteVia {

            @Before
            public void storeTextInClipboard() {
                interact(() -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(text);
                    Clipboard.getSystemClipboard().setContent(content);
                });
            }
            @After
            public void assertAndCleanup() {
                assertEquals(beginning + text + middle + end, area.getText());

                release(PASTE, SHORTCUT, V, SHIFT, INSERT);
            }

            @Test public void paste() {
                // this test fails on Linux; Windows is untested
                // so for now, only run on Mac
                // TODO: update if test succeeds on Windows, too
                run_only_on_mac();

                press(PASTE);
            }
            @Test public void shortcut_V()   { press(SHORTCUT, V); }
            @Test public void shift_Insert() { press(SHIFT, INSERT); }
        }
    }

    public class WhenTextIsSelected {

        int startMiddle = beginning.length();
        int endMiddle = startMiddle + middle.length();

        @Before
        public void selectMiddle() {
            area.selectRange(startMiddle, endMiddle);
            assertEquals(middle, area.getSelectedText());
        }

        public class SelectionIsStoredInClipboardWhenCopyVia {

            @After
            public void assertAndCleanup() {
                interact(() -> {
                    assertTrue(Clipboard.getSystemClipboard().hasString());
                    assertEquals(middle, Clipboard.getSystemClipboard().getString());
                });

                // cleanup
                release(SHORTCUT, COPY, C, INSERT);
            }

            @Test public void copy()            { press(COPY); }
            @Test public void shortcut_C()      { press(SHORTCUT, C); }
            @Test public void shortcut_Insert() { press(SHORTCUT, INSERT); }

        }

        public class SelectionIsRemovedAndStoredInClipboardWhenCutVia {

            @After
            public void assertAndCleanup() {
                assertEquals(beginning + end, area.getText());
                interact(() -> {
                    assertTrue(Clipboard.getSystemClipboard().hasString());
                    assertEquals(middle, Clipboard.getSystemClipboard().getString());
                });

                release(CUT, SHIFT, X, DELETE);
            }

            @Test public void cut()          {
                // this test fails on Linux; Windows is untested
                // so for now, only run on Mac
                // TODO: update if test succeeds on Windows, too
                run_only_on_mac();
                press(CUT);
            }
            @Test public void shortcut_X()   { press(SHORTCUT, X); }
            @Test public void shift_Delete() { press(SHIFT, DELETE); }

        }

        public class SelectionIsReplacedInAreaWhenPasteVia {

            @Before
            public void storeTextInClipboard() {
                interact(() -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(text);
                    Clipboard.getSystemClipboard().setContent(content);
                });
            }
            @After
            public void assertAndCleanup() {
                assertEquals(beginning + text + end, area.getText());

                release(PASTE, SHORTCUT, V, SHIFT, INSERT);
            }

            @Test public void paste()        {
                // this test fails on Linux; Windows is untested
                // so for now, only run on Mac
                // TODO: update if test succeeds on Windows, too
                run_only_on_mac();
                press(PASTE);
            }
            @Test public void shortcut_V()   { press(SHORTCUT, V); }
            @Test public void shift_Insert() { press(SHIFT, INSERT); }

        }
    }
}