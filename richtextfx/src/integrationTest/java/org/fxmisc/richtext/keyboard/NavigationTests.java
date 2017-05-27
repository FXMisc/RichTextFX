package org.fxmisc.richtext.keyboard;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.geometry.Bounds;
import javafx.stage.Stage;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static javafx.scene.input.KeyCode.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(NestedRunner.class)
public class NavigationTests {

    private int entityStart(int entityIndex, String[] array) {
        if (entityIndex == 0) {
            return 0;
        } else {
            return Arrays.stream(array)
                    .map(String::length)
                    .limit(entityIndex)
                    .reduce(0, (a, b) -> a + b)
                    + entityIndex; // for delimiter characters
        }
    }

    private int entityEnd(int entityIndex, String[] array, GenericStyledArea<?, ?, ?> area) {
        if (entityIndex == array.length - 1) {
            return area.getLength();
        } else {
            return entityStart(entityIndex + 1, array) - 1;
        }
    }

    public class SingleLineTests extends InlineCssTextAreaAppTest {

        String[] words = { "the", "cat", "can", "walk" };
        String fullText = String.join(" ", words);

        private int wordStart(int wordIndex) {
            return entityStart(wordIndex, words);
        }

        private int wordEnd(int wordIndex) {
            return entityEnd(wordIndex, words, area);
        }

        private void moveCaretTo(int position) {
            area.moveTo(position);
        }

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);
            stage.setWidth(300);
            area.replaceText(fullText);
        }

        public class NoModifiers {

            @Test
            public void left() {
                moveCaretTo(wordStart(1));
                assertTrue(area.getSelectedText().isEmpty());

                type(LEFT);

                assertEquals(wordEnd(0), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

            @Test
            public void right() {
                moveCaretTo(wordStart(1));
                assertTrue(area.getSelectedText().isEmpty());

                type(RIGHT);

                assertEquals(wordStart(1) + 1, area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

        }

        public class ShortcutDown {

            @Before
            public void setup() {
                press(SHORTCUT);
            }

            @Test
            public void left() {
                moveCaretTo(wordEnd(3));
                assertTrue(area.getSelectedText().isEmpty());

                // first left goes to boundary of current word
                type(LEFT);

                assertEquals(wordStart(3), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());

                // second left skips space and goes to end boundary of prev word
                type(LEFT);

                assertEquals(wordStart(2), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

            @Test
            public void right() {
                moveCaretTo(wordStart(0));
                assertTrue(area.getSelectedText().isEmpty());

                // first right goes to boundary of current word
                type(RIGHT);

                assertEquals(wordEnd(0), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());

                // second right skips space and goes to end boundary of next word
                type(RIGHT);

                assertEquals(wordEnd(1), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

            @Test
            public void a() {
                assertTrue(area.getSelectedText().isEmpty());

                type(A);

                assertEquals(area.getText(), area.getSelectedText());
            }

        }

        public class ShiftDown {

            @Before
            public void setup() {
                press(SHIFT);
            }

            @Test
            public void left() {
                moveCaretTo(wordStart(1));
                assertTrue(area.getSelectedText().isEmpty());

                type(LEFT);

                assertEquals(wordEnd(0), area.getCaretPosition());
                assertEquals(" ", area.getSelectedText());
            }

            @Test
            public void right() {
                moveCaretTo(wordEnd(0));
                assertTrue(area.getSelectedText().isEmpty());

                type(RIGHT);

                assertEquals(wordStart(1), area.getCaretPosition());
                assertEquals(" ", area.getSelectedText());
            }

        }

        public class ShortcutShiftDown {

            @Before
            public void setup() {
                press(SHORTCUT, SHIFT);
            }

            @Test
            public void left() {
                moveCaretTo(wordEnd(3));
                assertTrue(area.getSelectedText().isEmpty());

                // first left goes to boundary of current word
                type(LEFT);

                assertEquals(wordStart(3), area.getCaretPosition());
                assertEquals(words[3], area.getSelectedText());

                // second left skips space and goes to end boundary of prev word
                type(LEFT);

                assertEquals(wordStart(2), area.getCaretPosition());
                assertEquals(words[2] + " " + words[3], area.getSelectedText());
            }

            @Test
            public void right() {
                moveCaretTo(wordStart(0));
                assertTrue(area.getSelectedText().isEmpty());

                // first right goes to boundary of current word
                type(RIGHT);

                assertEquals(wordEnd(0), area.getCaretPosition());
                assertEquals(words[0], area.getSelectedText());

                // second right skips space and goes to end boundary of next word
                type(RIGHT);

                assertEquals(wordEnd(1), area.getCaretPosition());
                assertEquals(words[0] + " " + words[1], area.getSelectedText());
            }

        }

    }

    public class MultiLineTests extends InlineCssTextAreaAppTest {

        public final String[] lines = {
                "01 02 03 04 05",
                "11 12 13 14 15",
                "21 22 23 24 25",
                "31 32 33 34 35",
                "41 42 43 44 45"
        };

        private int lineStart(int lineIndex) {
            return entityStart(lineIndex, lines);
        }

        private int lineEnd(int lineIndex) {
            return entityEnd(lineIndex, lines, area);
        }

        String fullText = String.join(" ", lines);

        private void moveCaretTo(int position) {
            area.moveTo(position);
        }

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);
            area.setWrapText(true);
            area.replaceText(fullText);

            // insures area's text appears exactly as the declaration of `lines`
            stage.setWidth(150);
            area.setStyle(
                    "-fx-font-family: monospace;" +
                    "-fx-font-size: 12pt;"
            );
        }

        @Before
        public void setup() throws TimeoutException {
            // When the stage's width changes, TextFlow does not properly handle API calls to a
            //  multi-line paragraph immediately. So, wait until it correctly responds
            //  to the stage width change
            WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                    () -> area.getParagraphLinesCount(0) == lines.length
            );
        }

        public class NoModifiers {

            @Test
            public void up() {
                moveCaretTo(lineStart(2));
                assertTrue(area.getSelectedText().isEmpty());

                type(UP);

                assertEquals(lineStart(1), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

            @Test
            public void down() {
                moveCaretTo(lineStart(1));
                assertTrue(area.getSelectedText().isEmpty());

                type(DOWN);

                assertEquals(lineStart(2), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

            @Test
            public void home() {
                moveCaretTo(lineEnd(1));
                assertTrue(area.getSelectedText().isEmpty());

                type(HOME);

                assertEquals(lineStart(1), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

            @Test
            public void end() {
                moveCaretTo(lineStart(1));
                assertTrue(area.getSelectedText().isEmpty());

                type(END);

                assertEquals(lineEnd(1), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

        }

        public class ShortcutDown {

            @Before
            public void setup() {
                press(SHORTCUT);
            }

            // up/down do nothing
            @Test
            public void up() {
                assertTrue(area.getSelectedText().isEmpty());
                moveCaretTo(lineStart(2));

                type(UP);

                assertEquals(lineStart(2), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

            @Test
            public void down() {
                assertTrue(area.getSelectedText().isEmpty());
                moveCaretTo(lineStart(2));

                type(DOWN);

                assertEquals(lineStart(2), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

            @Test
            public void home() {
                moveCaretTo(lineStart(2));
                assertTrue(area.getSelectedText().isEmpty());

                type(HOME);

                assertEquals(0, area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

            @Test
            public void end() {
                moveCaretTo(lineStart(1));
                assertTrue(area.getSelectedText().isEmpty());

                type(END);

                assertEquals(area.getLength(), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

        }

        public class ShiftDown {

            @Before
            public void setup() {
                press(SHIFT);
            }

            @Test
            public void up() {
                moveCaretTo(lineStart(2));
                assertTrue(area.getSelectedText().isEmpty());

                type(UP);

                assertEquals(lineStart(1), area.getCaretPosition());
                assertEquals(lines[1] + " ", area.getSelectedText());
            }

            @Test
            public void down() {
                moveCaretTo(lineStart(1));
                assertTrue(area.getSelectedText().isEmpty());

                type(DOWN);

                assertEquals(lineStart(2), area.getCaretPosition());
                assertEquals(lines[1] + " ", area.getSelectedText());
            }

            @Test
            public void home() {
                moveCaretTo(lineEnd(1));
                assertTrue(area.getSelectedText().isEmpty());

                type(HOME);

                assertEquals(lineStart(1), area.getCaretPosition());
                assertEquals(lines[1], area.getSelectedText());
            }

            @Test
            public void end() {
                moveCaretTo(lineStart(1));
                assertTrue(area.getSelectedText().isEmpty());

                type(END);

                assertEquals(lineEnd(1), area.getCaretPosition());
                assertEquals(lines[1], area.getSelectedText());
            }

        }

        public class ShortcutShiftDown {

            @Before
            public void setup() {
                press(SHORTCUT, SHIFT);
            }

            @Test
            public void up() {
                moveCaretTo(lineStart(2));
                assertTrue(area.getSelectedText().isEmpty());

                type(UP);

                assertEquals(lineStart(2), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

            @Test
            public void down() {
                moveCaretTo(lineStart(1));
                assertTrue(area.getSelectedText().isEmpty());

                type(DOWN);

                assertEquals(lineStart(1), area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
            }

            @Test
            public void home() {
                moveCaretTo(area.getLength());
                assertTrue(area.getSelectedText().isEmpty());

                type(HOME);

                assertEquals(0, area.getCaretPosition());
                assertEquals(area.getText(), area.getSelectedText());
            }

            @Test
            public void end() {
                moveCaretTo(0);
                assertTrue(area.getSelectedText().isEmpty());

                type(END);

                assertEquals(area.getLength(), area.getCaretPosition());
                assertEquals(area.getText(), area.getSelectedText());
            }

        }

    }

    public class ViewportTests extends InlineCssTextAreaAppTest {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);

            // allow 6 lines to be displayed
            stage.setHeight(90);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(i).append("\n");
            }
            sb.append(9);
            area.replaceText(sb.toString());
        }

        @Test
        public void testPageUp() {
            interact(() -> {
                area.moveTo(5, 0);
                area.requestFollowCaret();
            });
            assertTrue(area.getSelectedText().isEmpty());
            Bounds beforeBounds = area.getCaretBounds().get();

            type(PAGE_UP);

            Bounds afterBounds = area.getCaretBounds().get();
            assertEquals(0, area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
            assertTrue(beforeBounds.getMinY() > afterBounds.getMinY());
        }

        @Ignore("doesn't work despite 'testShiftPageDown' working fine using the same code")
        @Test
        public void testPageDown() throws Exception {
            interact(() -> {
                area.moveTo(0);
                area.requestFollowCaret();
            });
            assertTrue(area.getSelectedText().isEmpty());
            Bounds beforeBounds = area.getCaretBounds().get();

            type(PAGE_DOWN);

            Bounds afterBounds = area.getCaretBounds().get();
            assertEquals(area.getAbsolutePosition(5, 0), area.getCaretPosition());
            assertTrue(area.getSelectedText().isEmpty());
            assertTrue(beforeBounds.getMinY() < afterBounds.getMinY());
        }

        @Test
        public void testShiftPageUp() {
            interact(() -> {
                area.moveTo(5, 0);
                area.requestFollowCaret();
            });
            assertTrue(area.getSelectedText().isEmpty());
            Bounds beforeBounds = area.getCaretBounds().get();

            press(SHIFT).type(PAGE_UP).release(SHIFT);

            Bounds afterBounds = area.getCaretBounds().get();
            assertEquals(0, area.getCaretPosition());
            assertEquals(area.getText(0, 0, 5, 0), area.getSelectedText());
            assertTrue(beforeBounds.getMinY() > afterBounds.getMinY());
        }

        @Test
        public void testShiftPageDown() {
            interact(() -> {
                area.moveTo(0);
                area.requestFollowCaret();
            });
            assertTrue(area.getSelectedText().isEmpty());
            Bounds beforeBounds = area.getCaretBounds().get();

            press(SHIFT).type(PAGE_DOWN).release(SHIFT);

            Bounds afterBounds = area.getCaretBounds().get();
            assertEquals(area.getAbsolutePosition(5, 0), area.getCaretPosition());
            assertEquals(area.getText(0, 0, 5, 0), area.getSelectedText());
            assertTrue(beforeBounds.getMinY() < afterBounds.getMinY());
        }

    }
}
