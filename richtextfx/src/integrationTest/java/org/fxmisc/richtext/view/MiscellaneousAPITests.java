package org.fxmisc.richtext.view;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.geometry.Bounds;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.ViewActions.CaretVisibility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(NestedRunner.class)
public class MiscellaneousAPITests {

    public class CharacterBoundsTest extends InlineCssTextAreaAppTest {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);
            area.replaceText("a line of sample text");
        }

        @Test
        public void selectionBoundsUnchangedWhenCallGetCharacterBounds() {
            area.selectAll();
            Bounds bounds = area.getSelectionBounds().get();

            // getCharacterBoundsOnScreen() uses the selection shape to calculate the bounds
            // so insure it doesn't affect the selection shape if something is selected
            // before it gets called
            area.getCharacterBoundsOnScreen(0, area.getLength() - 1);

            assertEquals(bounds, area.getSelectionBounds().get());
        }

    }

    public class ParagraphLinesCountTests extends InlineCssTextAreaAppTest {

        @Test
        public void multiLineReturnsCorrectCount() {
            String[] lines = {
                    "01 02 03 04 05",
                    "11 12 13 14 15",
                    "21 22 23 24 25"
            };
            interact(() -> {
                area.setWrapText(true);
                stage.setWidth(120);
                area.replaceText(String.join(" ", lines));
            });

            assertEquals(3, area.getParagraphLinesCount(0));
        }

        @Test
        public void singleLineReturnsOne() {
            interact(() -> area.replaceText("some text"));
            assertFalse(area.isWrapText());

            assertEquals(1, area.getParagraphLinesCount(0));
        }

    }

    public class MoveCaretTests extends InlineCssTextAreaAppTest {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);

            // insure caret is always visible
            area.setShowCaret(CaretVisibility.ON);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                sb.append(i).append("\n");
            }
            area.replaceText(sb.toString());
            area.moveTo(0);
            area.showParagraphAtTop(0);
        }

        @Test
        public void testMoveCaretAndFollowIt() {
            assertTrue(area.getCaretBounds().isPresent());

            // move caret outside of viewport
            area.moveTo(area.getLength());
            area.requestFollowCaret();

            // needed for test to pass
            WaitForAsyncUtils.waitForFxEvents();

            // viewport should update itself so caret is visible again
            assertTrue(area.getCaretBounds().isPresent());
        }

        @Test
        public void testMoveCaretWithoutFollowingIt() {
            assertTrue(area.getCaretBounds().isPresent());

            // move caret outside of viewport
            area.moveTo(area.getLength());

            // caret should not be visible
            assertFalse(area.getCaretBounds().isPresent());
        }
    }
}
